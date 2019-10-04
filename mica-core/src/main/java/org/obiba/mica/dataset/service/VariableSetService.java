/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import com.google.common.collect.Lists;
import com.googlecode.protobuf.format.JsonFormat;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.OpalTable;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetCategory;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.opal.web.model.Magma;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class VariableSetService extends DocumentSetService {

  private final PublishedDatasetVariableService publishedDatasetVariableService;

  private final PublishedDatasetService publishedDatasetService;

  @Inject
  public VariableSetService(
    PublishedDatasetVariableService publishedDatasetVariableService,
    PublishedDatasetService publishedDatasetService) {
    this.publishedDatasetVariableService = publishedDatasetVariableService;
    this.publishedDatasetService = publishedDatasetService;
  }

  @Override
  public String getType() {
    return DatasetVariable.MAPPING_NAME;
  }

  @Override
  public List<String> extractIdentifiers(String importedIdentifiers) {
    return extractIdentifiers(importedIdentifiers,
      id -> {
        DatasetVariable.Type type = DatasetVariable.IdResolver.from(id).getType();
        return DatasetVariable.Type.Collected.equals(type) || DatasetVariable.Type.Dataschema.equals(type);
      });
  }

  /**
   * Get variables from their identifiers.
   *
   * @param identifiers
   * @return
   */
  public List<DatasetVariable> getVariables(Set<String> identifiers) {
    return getVariables(identifiers, true);
  }

  /**
   * Get variables from their identifiers.
   *
   * @param identifiers
   * @param useCache
   * @return
   */
  public List<DatasetVariable> getVariables(Set<String> identifiers, boolean useCache) {
    return publishedDatasetVariableService.findByIds(Lists.newArrayList(identifiers), useCache);
  }

  /**
   * Get the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @return
   */
  public List<DatasetVariable> getVariables(DocumentSet documentSet) {
    return getVariables(documentSet, true);
  }

  /**
   * Get the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @param useCache
   * @return
   */
  public List<DatasetVariable> getVariables(DocumentSet documentSet, boolean useCache) {
    ensureType(documentSet);
    if (documentSet.getIdentifiers().isEmpty()) return Lists.newArrayList();
    return getVariables(documentSet.getIdentifiers(), useCache);
  }

  /**
   * Get a subset of the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @param from
   * @param limit
   * @return
   */
  public List<DatasetVariable> getVariables(DocumentSet documentSet, int from, int limit) {
    ensureType(documentSet);
    if (documentSet.getIdentifiers().isEmpty()) return Lists.newArrayList();
    List<String> ids = Lists.newArrayList(documentSet.getIdentifiers());
    Collections.sort(ids);
    int to = from + limit;
    if (to > ids.size()) to = ids.size();
    return publishedDatasetVariableService.findByIds(ids.subList(from, to));
  }

  public void createOpalViewsZip(List<DatasetVariable> variables, MicaConfig.OpalViewsGrouping opalViewsGrouping, OutputStream outputStream) throws IOException {
    List<Magma.ViewDto> views = createOpalViews(variables, opalViewsGrouping);

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
      for (Magma.ViewDto view : views) {
        zipOutputStream.putNextEntry(new ZipEntry(view.getName() + ".json"));
        zipOutputStream.write(JsonFormat.printToString(view).getBytes());
        zipOutputStream.closeEntry();
      }
    }
  }

  /**
   * Zip View dtos
   *
   * @param documentSet a set of variables, source for opal views
   * @param outputStream the outputstream
   * @throws IOException in case the zip fails
   */
  public void createOpalViewsZip(DocumentSet documentSet, MicaConfig.OpalViewsGrouping opalViewsGrouping, OutputStream outputStream) throws IOException {
    createOpalViewsZip(getVariables(documentSet), opalViewsGrouping, outputStream);
  }

  /**
   * Get a list of opal view dto for backup restore purposes
   *
   * @param variables The source variables set
   * @return a list of opal views grouped by project and entity type
   */
  private List<Magma.ViewDto> createOpalViews(List<DatasetVariable> variables, MicaConfig.OpalViewsGrouping opalViewsGrouping) {
    List<Magma.ViewDto> views = new ArrayList<>();

    List<Dataset> datasets = publishedDatasetService.findByIds(variables.stream()
      .map(DatasetVariable::getDatasetId)
      .distinct()
      .collect(Collectors.toList())
    );

    Map<String, String> opalTableFullNameMap = datasets.stream().collect(Collectors.toMap(Dataset::getId, this::toOpalTableFullName));

    switch (opalViewsGrouping) {
      case PROJECT_TABLE:
        Map<String, List<DatasetVariable>> variablesGroupedByProjectTable = variables.stream()
          .collect(Collectors.groupingBy(variable -> opalTableFullNameMap.get(variable.getDatasetId())));

        for (Entry<String, List<DatasetVariable>> entry : variablesGroupedByProjectTable.entrySet()) {
          views.add(createViewDto(entry.getValue(), entry.getKey()));
        }
        break;
      case PROJECT_ENTITY_TYPE:
        Map<String, List<DatasetVariable>> variablesGroupedByProjectEntityType = variables.stream()
          .collect(Collectors.groupingBy(variable -> {
            String opalTableFullName = opalTableFullNameMap.get(variable.getDatasetId());
            return opalTableFullName.split("\\.")[0] + "." + variable.getEntityType();
          }));

        for (Entry<String, List<DatasetVariable>> entry : variablesGroupedByProjectEntityType.entrySet()) {
          views.add(createViewDto(entry.getValue(), entry.getKey(), opalTableFullNameMap));
        }
        break;
      case ENTITY_TYPE:
        Map<String, List<DatasetVariable>> variablesGroupedByEntityType = variables.stream()
          .collect(Collectors.groupingBy(DatasetVariable::getEntityType));

        for (Entry<String, List<DatasetVariable>> entry : variablesGroupedByEntityType.entrySet()) {
          views.add(createViewDto(entry.getValue(), entry.getKey(), opalTableFullNameMap));
        }
        break;
    }

    return views;
  }

  // group by entity type
  private Magma.ViewDto createViewDto(List<DatasetVariable> variables, String opalTableFullName) {
    Magma.ViewDto.Builder builder = Magma.ViewDto.newBuilder();
    builder.setExtension(
      Magma.VariableListViewDto.view,
      Magma.VariableListViewDto.newBuilder()
        .addAllVariables(variables.stream().map(variable -> toVariableDto(variable, opalTableFullName)).collect(Collectors.toList())).build());

    builder.addFrom(opalTableFullName);
    builder.setName(opalTableFullName.replaceAll("\\.", "_") + "-view-" + new Date().getTime());

    return builder.build();
  }

  private Magma.ViewDto createViewDto(List<DatasetVariable> variables, String key, Map<String, String> opalTableFullNameMap) {
    Magma.ViewDto.Builder builder = Magma.ViewDto.newBuilder();

    Set<String> usedOpalTables = new HashSet<>();

    builder.setExtension(
      Magma.VariableListViewDto.view,
      Magma.VariableListViewDto.newBuilder()
        .addAllVariables(variables.stream().map(variable -> {
          String opalTableFullName = opalTableFullNameMap.get(variable.getDatasetId());
          usedOpalTables.add(opalTableFullName);
          return toVariableDto(variable, opalTableFullName);
        }).collect(Collectors.toList())).build());

    builder.addAllFrom(usedOpalTables);
    builder.setName(key.replaceAll("\\.", "_") + "-view-" + new Date().getTime());

    return builder.build();
  }

  private Magma.VariableDto toVariableDto(DatasetVariable datasetVariable, String opalTableFullName) {
    Magma.VariableDto.Builder builder = Magma.VariableDto.newBuilder();

    builder.setName(datasetVariable.getName());
    builder.setIndex(datasetVariable.getIndex());
    builder.setReferencedEntityType(datasetVariable.getReferencedEntityType());
    builder.setUnit(datasetVariable.getUnit());
    builder.setMimeType(datasetVariable.getMimeType());
    builder.setIsRepeatable(datasetVariable.isRepeatable());
    builder.setOccurrenceGroup(datasetVariable.getOccurrenceGroup());
    builder.setValueType(datasetVariable.getValueType());
    builder.setEntityType(datasetVariable.getEntityType());

    if (datasetVariable.hasAttributes()) {
      builder.addAllAttributes(toAttributeDtoList(datasetVariable.getAttributes()));
    }

    if (datasetVariable.hasCategories()) {
      builder.addAllCategories(toCategoryDtoList(datasetVariable.getCategories()));
    }

    String[] split = opalTableFullName.split("\\.");
    builder.addAttributes(Magma.AttributeDto.newBuilder().setName("derivedFrom").setNamespace("opal").setValue("/datasource/" + split[0] + "/table/" + split[1] + "/variable/" + datasetVariable.getName()).build());
    builder.addAttributes(Magma.AttributeDto.newBuilder().setName("script").setValue("$('" + datasetVariable.getName() + "')").build());

    return builder.build();
  }

  private List<Magma.AttributeDto> toAttributeDtoList(Attributes attributes) {
    return attributes.asAttributeList().stream().map(attribute -> {
      Magma.AttributeDto.Builder builder = Magma.AttributeDto.newBuilder();

      builder.setName(attribute.getName());

      if (attribute.hasNamespace()) builder.setNamespace(attribute.getNamespace());

      LocalizedString values = attribute.getValues();
      String firstKey = values.firstKey();

      if (!"und".equals(firstKey)) builder.setLocale(firstKey);

      builder.setValue(values.get(firstKey));

      return builder.build();
    }).collect(Collectors.toList());
  }


  private List<Magma.CategoryDto> toCategoryDtoList(List<DatasetCategory> categories) {
    return categories.stream().map(category -> {
      Magma.CategoryDto.Builder builder = Magma.CategoryDto.newBuilder();

      builder.setName(category.getName());
      builder.setIsMissing(category.isMissing());

      Attributes categoryAttributes = category.getAttributes();
      if (categoryAttributes != null && !categoryAttributes.isEmpty()) builder.addAllAttributes(
        toAttributeDtoList(categoryAttributes));

      return builder.build();
    }).collect(Collectors.toList());
  }

  private String toOpalTableFullName(Dataset dataset) {
    OpalTable opalTable = dataset instanceof StudyDataset ? ((StudyDataset) dataset).getSafeStudyTable() : ((HarmonizationDataset) dataset).getSafeHarmonizationTable();
    return opalTable.getProject() + "." + opalTable.getTable();
  }
}
