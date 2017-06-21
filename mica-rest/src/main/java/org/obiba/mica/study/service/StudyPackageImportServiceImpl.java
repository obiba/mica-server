/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.commons.math3.util.Pair;
import org.bson.types.ObjectId;
import org.obiba.jersey.protobuf.AbstractProtobufProvider;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StudyPackageImportServiceImpl extends AbstractProtobufProvider implements StudyPackageImportService {

  private static final Logger log = LoggerFactory.getLogger(StudyPackageImportServiceImpl.class);

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private TempFileService tempFileService;

  @Inject
  private CollectionStudyService collectionStudyService;

  @Inject
  private NetworkService networkService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private Dtos dtos;

  @Override
  public void importZip(InputStream inputStream, boolean publish) throws IOException {
    final StudyPackage studyPackage = new StudyPackage(inputStream);
    if(studyPackage.study != null) {
      Map<String, ByteSource> dict = studyPackage.attachments.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      Optional.ofNullable(studyPackage.study.getLogo()).ifPresent(a -> saveAttachmentTempFile(dict, a));
      Set<String> attachmentIds = Sets.newHashSet();

      studyPackage.studyAttachments.forEach(a -> {
        if(attachmentIds.contains(a.getId())) {
          String origId = a.getId();
          a.setId(new ObjectId().toString());
          saveAttachmentTempFile(dict, a, origId);
        } else {
          saveAttachmentTempFile(dict, a);
          attachmentIds.add(a.getId());
        }
      });

      importStudy(studyPackage.study, studyPackage.studyAttachments, publish);

      for(Network net : studyPackage.networks) {
        importNetwork(net, publish, studyPackage);
      }

      studyPackage.datasets.forEach(ds -> importDataset(ds, publish));
    }
  }

  private void saveAttachmentTempFile(Map<String, ByteSource> dict, Attachment a) {
    saveAttachmentTempFile(dict, a, a.getId());
  }

  private void saveAttachmentTempFile(Map<String, ByteSource> dict, Attachment a, String aid) {
    if(dict.containsKey(aid)) {
      try {
        saveTempFile(a, dict.get(aid));
      } catch(IOException e) {
        Throwables.propagate(e);
      }
    }
  }

  private void importStudy(Study study, List<Attachment> attachments, boolean publish) {
    if(study.getAcronym() == null) {
      study.setAcronym(study.getName().asAcronym());
    }
    study.getAcronym().entrySet().forEach(entry -> {
      if(study.getName().containsKey(entry.getKey())) {
        String newName = study.getName().get(entry.getKey()).replace("(" + entry.getValue() + ")", "").trim();
        study.getName().put(entry.getKey(), newName);
      }
    });

    collectionStudyService.save(study, "Imported");

    attachments.forEach(a -> {
      a.setPath(String.format(a.getPath(), study.getId()));
      fileSystemService.save(a);
    });

    if(publish) {
      collectionStudyService.publish(study.getId(), true, PublishCascadingScope.ALL);
    }
  }

  private void importNetwork(Network network, boolean publish, StudyPackage studyPackage) throws IOException {
    Network updated;
    try {
      Network existing = networkService.findById(network.getId());
      network.getStudyIds().stream().filter(sid -> !existing.getStudyIds().contains(sid))
        .forEach(sid -> existing.getStudyIds().add(sid));
      updated = existing;
    } catch(NoSuchNetworkException e) {
      updated = network;
    }

    for(Map.Entry<String, ByteSource> e : studyPackage.attachments.entrySet()) {
      Attachment attachment = network.getLogo();

      if(attachment != null && attachment.getId().equals(e.getKey())) {
        saveTempFile(attachment, e.getValue());
        updated.setLogo(attachment);
      }
    }

    networkService.save(updated);

    if(publish) networkService.publish(updated.getId(), true, PublishCascadingScope.ALL);
  }

  private void saveTempFile(Attachment attachment, ByteSource content) throws IOException {
    TempFile tempFile = new TempFile();
    tempFile.setId(attachment.getId());
    tempFile.setName(attachment.getName());
    tempFileService.addTempFile(tempFile, content.openStream());
    attachment.setMd5(content.hash(Hashing.md5()).toString());
    attachment.setSize(content.size());
  }

  private void importDataset(Dataset dataset, boolean publish) {
    if(dataset instanceof StudyDataset) {
      importDataset((StudyDataset) dataset, publish);
    } else {
      importDataset((HarmonizationDataset) dataset, publish);
    }
  }

  private void importDataset(StudyDataset dataset, boolean publish) {
    if(!dataset.hasStudyTable() || Strings.isNullOrEmpty(dataset.getStudyTable().getStudyId())) return;
    try {
      studyDatasetService.findById(dataset.getId());
      studyDatasetService.save(dataset);
    } catch(NoSuchDatasetException e) {
      studyDatasetService.save(dataset);
    }
    if(publish) studyDatasetService.publish(dataset.getId(), publish, PublishCascadingScope.ALL);
  }

  private void importDataset(HarmonizationDataset dataset, boolean publish) {
    try {
      HarmonizationDataset existing = harmonizationDatasetService.findById(dataset.getId());
      // TODO merge study tables
      harmonizationDatasetService.save(existing);
    } catch(NoSuchDatasetException e) {
      harmonizationDatasetService.save(dataset);
    }
    if(publish) harmonizationDatasetService.publish(dataset.getId(), publish, PublishCascadingScope.ALL);
  }

  private final class StudyPackage {

    private Study study = null;

    private List<Attachment> studyAttachments = null;

    private final List<Network> networks = Lists.newArrayList();

    private final List<Dataset> datasets = Lists.newArrayList();

    private final Map<String, ByteSource> attachments = Maps.newHashMap();

    private StudyPackage(InputStream inputStream) {
      try(ZipInputStream zipIn = new ZipInputStream(inputStream)) {
        ZipEntry entry;
        while((entry = zipIn.getNextEntry()) != null) {
          readZipEntry(zipIn, entry);
        }
        makeIdMapping();
      } catch(Exception e) {
        log.error("Failed importing from zip", e);
        throw new RuntimeException("Failed importing from zip", e);
      }
    }

    private void readZipEntry(ZipInputStream zipIn, ZipEntry entry) throws IOException {
      if(entry.getName().endsWith("attachments/") || entry.getSize() == 0) {
        zipIn.closeEntry();
        return;
      }

      if(entry.getName().contains("attachments/")) {
        String attId = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
        attachments.put(attId, ByteSource.wrap(readBytes(zipIn)));
      } else if(entry.getName().endsWith(".json")) {
        String name = entry.getName();
        int slash = name.lastIndexOf('/');

        if(slash > -1) {
          name = name.substring(slash + 1);
        }

        log.debug("Reading {}...", name);

        if(name.startsWith("study-")) {
          Pair<Study, List<Attachment>> studyInput = readStudy(zipIn);
          study = studyInput.getFirst();
          studyAttachments = studyInput.getSecond();
        } else if(name.startsWith("dataset-")) {
          datasets.add(readDataset(zipIn));
        } else if(name.startsWith("network-")) {
          networks.add(readNetwork(zipIn));
        }

        zipIn.closeEntry();
      }
    }

    private void makeIdMapping() {
      study.setAcronym(ensureAcronym(study.getAcronym(), study.getName()));
      String sId = study.getAcronym().asUrlSafeString().replace("(", "").replace(")", "").toLowerCase();
      study.setId(sId);
      study.setOpal(null);
      for(Network network : networks) {
        network.setAcronym(ensureAcronym(network.getAcronym(), network.getName()));
        String nId = network.getAcronym().asUrlSafeString().toLowerCase();
        network.setId(nId);
        network.setStudyIds(Lists.newArrayList(study.getId()));
      }
    }

    private LocalizedString ensureAcronym(LocalizedString acronym, LocalizedString name) {
      if(acronym == null || acronym.isEmpty()) {
        return name.asAcronym();
      }
      return acronym;
    }

    private Pair<Study, List<Attachment>> readStudy(InputStream inputStream) throws IOException {
      Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
      Readable input = new InputStreamReader(inputStream, Charsets.UTF_8);
      JsonFormat.merge(input, builder);
      List<Attachment> atts = extractAttachments(builder);
      Study study = dtos.fromDto(builder);
      return Pair.create(study, atts);
    }

    /**
     * Extract the attachments in a separate list and rebuild the population and data collection event IDs.
     *
     * @param builder
     * @return
     */
    private List<Attachment> extractAttachments(Mica.StudyDto.Builder builder) {
      List<Attachment> atts = Lists.newArrayList();
      int pIdx = 1;
      for (Mica.PopulationDto.Builder pBuilder : builder.getPopulationsBuilderList()) {
        pBuilder.setId("" + pIdx++);
        int dceIdx = 1;
        for (Mica.PopulationDto.DataCollectionEventDto.Builder dceBuilder : pBuilder.getDataCollectionEventsBuilderList()) {
          dceBuilder.setId("" + dceIdx++);
          dceBuilder.getAttachmentsList().stream().map(dtos::fromDto).forEach(a -> {
            a.setPath("/collection-study/%s/population/" + pBuilder.getId() + "/data-collection-event/" + dceBuilder.getId());
            atts.add(a);
          });
        }
      }

      return atts;
    }

    private Network readNetwork(InputStream inputStream) throws IOException {
      Mica.NetworkDto.Builder builder = Mica.NetworkDto.newBuilder();
      Readable input = new InputStreamReader(inputStream, Charsets.UTF_8);
      JsonFormat.merge(input, builder);
      return dtos.fromDto(builder);
    }

    private Dataset readDataset(InputStream inputStream) throws IOException {
      Mica.DatasetDto.Builder builder = Mica.DatasetDto.newBuilder();
      Readable input = new InputStreamReader(inputStream, Charsets.UTF_8);
      JsonFormat.merge(input, builder);
      return dtos.fromDto(builder);
    }

    private byte[] readBytes(ZipInputStream zipIn) throws IOException {
      ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
      ByteStreams.copy(zipIn, entryOut);
      entryOut.close();
      return entryOut.toByteArray();
    }
  }
}
