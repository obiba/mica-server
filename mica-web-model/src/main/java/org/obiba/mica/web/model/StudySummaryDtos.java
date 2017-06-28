/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toSet;

@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class StudySummaryDtos {

  private static final Logger logger = LoggerFactory.getLogger(StudySummaryDtos.class);

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private PublishedDatasetVariableService datasetVariableService;

  @Inject
  private StudyService studyService;

  @NotNull
  public Mica.StudySummaryDto.Builder asDtoBuilder(@NotNull BaseStudy study) {
    EntityState studyState = studyService.getEntityState(study.getId());

    if(studyState.isPublished()) {
      return asDtoBuilder(study, studyState.isPublished(), datasetVariableService.getCountByStudyId(study.getId()));
    }

    return asDtoBuilder(study, studyState.isPublished(), 0);
  }

  @NotNull
  public Mica.StudySummaryDto.Builder asDtoBuilder(@NotNull BaseStudy study, boolean isPublished, long variablesCount) {
    Mica.StudySummaryDto.Builder builder;

    if (study instanceof Study) {
      builder = asCollectionStudyDtoBuilder((Study) study, isPublished, variablesCount);
    } else {
      builder = asHarmonizationStudyDtoBuilder((HarmonizationStudy) study, true, variablesCount);
    }

    builder.setStudyResourcePath(study.getResourcePath());
    return builder;
  }

  @NotNull
  public Mica.StudySummaryDto.Builder asCollectionStudyDtoBuilder(@NotNull Study study, boolean isPublished, long variablesCount) {
    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();
    builder.setPublished(isPublished);

    builder.setId(study.getId()) //
      .setTimestamps(TimestampsDtos.asDto(study)) //
      .addAllName(localizedStringDtos.asDto(study.getName())) //
      .addAllAcronym(localizedStringDtos.asDto(study.getAcronym())) //
      .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()))
      .setVariables(isPublished ? variablesCount : 0);

    if(study.getLogo() != null) builder.setLogo(attachmentDtos.asDto(study.getLogo()));

    addDesignInBuilderIfPossible(study.getModel(), builder);
    addTargetNumberInBuilderIfPossible(study.getModel(), builder);

    Collection<String> countries = new HashSet<>();
    SortedSet<Population> populations = study.getPopulations();

    if(populations != null) {
      countries.addAll(extractCountries(populations));
      populations.forEach(population -> builder.addPopulationSummaries(asDto(population)));
    }

    builder.setPermissions(permissionsDtos.asDto(study));

    builder.addAllCountries(countries);

    return builder;
  }

  @NotNull
  public Mica.StudySummaryDto.Builder asHarmonizationStudyDtoBuilder(@NotNull HarmonizationStudy study,
    boolean isPublished, long variablesCount) {

    Mica.StudySummaryDto.Builder builder = Mica.StudySummaryDto.newBuilder();
    builder.setPublished(isPublished);

    builder.setId(study.getId()) //
      .setTimestamps(TimestampsDtos.asDto(study)) //
      .addAllName(localizedStringDtos.asDto(study.getName())) //
      .addAllAcronym(localizedStringDtos.asDto(study.getAcronym())) //
      .addAllObjectives(localizedStringDtos.asDto(study.getObjectives()))
      .setVariables(isPublished ? variablesCount : 0);

    if(study.getLogo() != null) builder.setLogo(attachmentDtos.asDto(study.getLogo()));
    SortedSet<Population> populations = study.getPopulations();

    if(populations != null) {
      populations.forEach(population -> builder.addPopulationSummaries(asDto(population)));
    }

    builder.setPermissions(permissionsDtos.asDto(study));

    return builder;
  }

  Set<String> extractCountries(Collection<Population> populations) {

    return populations.stream().filter(Population::hasModel)
      .flatMap(p -> {
         if(p.getModel().get("selectionCriteria") instanceof Map) { //TODO: serialization should not include JsonTypeInfo to avoid this check.
           Map<String, Object> sc = (Map<String, Object>) p.getModel().get("selectionCriteria");
           List<String> countriesIso = sc.containsKey("countriesIso") ? (List<String>)sc.get("countriesIso") : null;
           return countriesIso != null ? countriesIso.stream() : Stream.empty();
         } else {
           return Optional.ofNullable((Population.SelectionCriteria) p.getModel().get("selectionCriteria"))
             .flatMap(sc -> Optional.ofNullable(sc.getCountriesIso().stream()))
             .orElseGet(Stream::empty);
         }
        }
      ).filter(Objects::nonNull)
      .collect(toSet());
  }

  void addTargetNumberInBuilderIfPossible(Map<String, Object> studyModel, Mica.StudySummaryDto.Builder builder) {
    try {
      extractStudyNumberOfParticipantsParticipantFromModel(studyModel)
        .ifPresent(numberOfParticipants ->
          builder.setTargetNumber(Mica.TargetNumberDto.newBuilder()
            .setNumber((Integer) numberOfParticipants.get("number"))
            .setNoLimit((Boolean) numberOfParticipants.get("noLimit")))
        );
    } catch (NullPointerException | ClassCastException | NoSuchElementException e) {
      logger.debug(String.format("Impossible to extract numberOfParticipant in model [%s]", studyModel), e);
    }
  }

  void addDesignInBuilderIfPossible(Map<String, Object> studyModel, Mica.StudySummaryDto.Builder builder) {
    try {
      extractStudyDesignFromModel(studyModel)
        .ifPresent(builder::setDesign);
    } catch (NullPointerException | ClassCastException e) {
      logger.debug(String.format("Impossible to extract studyDesign in model [%s]", studyModel), e);
    }
  }

  private Optional<String> extractStudyDesignFromModel(Map<String, Object> model) {
    if (model != null && model.containsKey("methods"))
      return Optional.ofNullable((String) ((Map<String, Object>) model.get("methods")).get("design"));
    else
      return Optional.empty();
  }

  private Optional<Map<String, Object>> extractStudyNumberOfParticipantsParticipantFromModel(Map<String, Object> model) {
    if (model != null && model.containsKey("numberOfParticipants")) {
      return Optional.ofNullable((Map<String, Object>) ((Map<String, Object>) model.get("numberOfParticipants")).get("participant"));
    } else {
      return Optional.empty();
    }
  }

  @NotNull
  Mica.PopulationSummaryDto asDto(@NotNull Population population) {
    Mica.PopulationSummaryDto.Builder builder = Mica.PopulationSummaryDto.newBuilder();

    builder.setId(population.getId()) //
      .addAllName(localizedStringDtos.asDto(population.getName()));

    if(population.getDataCollectionEvents() != null) {
      population.getDataCollectionEvents().forEach(dce -> builder.addDataCollectionEventSummaries(asDto(dce)));
    }

    return builder.build();
  }

  @NotNull
  Mica.DataCollectionEventSummaryDto asDto(@NotNull DataCollectionEvent dce) {
    return Mica.DataCollectionEventSummaryDto.newBuilder().setId(dce.getId()) //
      .addAllName(localizedStringDtos.asDto(dce.getName())).build();
  }

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull BaseStudy study) {
    return asDtoBuilder(study).build();
  }

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull BaseStudy study, @NotNull EntityState studyState) {
    Mica.EntityStateDto.Builder stateBuilder = Mica.EntityStateDto.newBuilder()
      .setRevisionsAhead(studyState.getRevisionsAhead()) //
      .setRevisionStatus(studyState.getRevisionStatus().name());

    if(studyState.isPublished()) {
      stateBuilder.setPublishedTag(studyState.getPublishedTag());
      if(studyState.hasPublishedId()) {
        stateBuilder.setPublishedId(studyState.getPublishedId());
      }
      if(studyState.hasPublicationDate()) {
        stateBuilder.setPublicationDate(studyState.getPublicationDate().toString());
        stateBuilder.setPublishedBy(studyState.getPublishedBy());
      }
    }

    stateBuilder.setPermissions(permissionsDtos.asDto(studyState));

    Mica.StudySummaryDto.Builder builder;

    if(study == null) {
      builder = Mica.StudySummaryDto.newBuilder();
      builder.setId(studyState.getId())
        .setTimestamps(TimestampsDtos.asDto(studyState));
    } else {
      builder = asDtoBuilder(study, studyState.isPublished(), 0);
    }

    builder.setPublished(studyState.isPublished());

    return builder.setExtension(Mica.EntityStateDto.studySummaryState, stateBuilder.build()).build();
  }

  @NotNull
  Mica.StudySummaryDto asDto(@NotNull EntityState studyState) {
    return asDto(studyService.findStudy(studyState.getId()), studyState);
  }

  Mica.StudySummaryDto asHarmoStudyDto(String studyId) {
    return asDto(studyId);
  }

  Mica.StudySummaryDto asDto(String studyId) {
    EntityState studyState = studyService.getEntityState(studyId);

    if (studyState.isPublished()) {
      BaseStudy study = publishedStudyService.findById(studyId);
      if (study != null) {
        return asDtoBuilder(study, true, datasetVariableService.getCountByStudyId(studyId)).build();
      }
    }

    return asDto(studyState);
  }
}
