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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.HarmonizationTable;
import org.obiba.mica.core.domain.NetworkTable;
import org.obiba.mica.core.domain.OpalTable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetCategory;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;


@Component
class DatasetDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttributeDtos attributeDtos;

  @Inject
  private EntityStateDtos entityStateDtos;

  @Inject
  private StudySummaryDtos studySummaryDtos;

  @Inject
  private NetworkSummaryDtos networkSummaryDtos;

  @Inject
  private PermissionsDtos permissionsDtos;

  @Inject
  private TaxonomyDtos taxonomyDtos;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private SubjectAclService subjectAclService;

  @NotNull
  Mica.DatasetDto.Builder asDtoBuilder(@NotNull StudyDataset dataset, boolean asDraft) {
    Mica.DatasetDto.Builder builder = asBuilder(dataset);
    builder.setVariableType(DatasetVariable.Type.Collection.name());

    if(dataset.hasStudyTable() && !Strings.isNullOrEmpty(dataset.getStudyTable().getStudyId())) {
      Mica.CollectionDatasetDto.Builder sbuilder = Mica.CollectionDatasetDto.newBuilder()//
        .setStudyTable(asDto(dataset.getStudyTable(), true));
      builder.setExtension(Mica.CollectionDatasetDto.type, sbuilder.build());
    }

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(dataset);

    if(asDraft) {
      StudyDatasetState state = studyDatasetStateRepository.findOne(dataset.getId());
      if(state != null) {
        builder.setPublished(state.isPublished());
        builder.setExtension(Mica.EntityStateDto.datasetState,
          entityStateDtos.asDto(state).setPermissions(permissionsDto).build());
      }
    }

    builder.setPermissions(permissionsDto);
    return builder;
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull StudyDataset dataset) {
    return asDto(dataset, false);
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull StudyDataset dataset, boolean asDraft) {
    return asDtoBuilder(dataset, asDraft).build();
  }

  @NotNull
  Mica.DatasetDto.Builder asDtoBuilder(@NotNull HarmonizationDataset dataset, boolean asDraft) {
    Mica.DatasetDto.Builder builder = asBuilder(dataset);
    builder.setVariableType(DatasetVariable.Type.Dataschema.name());

    Mica.HarmonizationDatasetDto.Builder hbuilder = Mica.HarmonizationDatasetDto.newBuilder();

    String networkId = dataset.getNetworkId();
    if(!Strings.isNullOrEmpty(networkId)) {

      if (asDraft) {
        if (subjectAclService.isPermitted("/draft/network", "VIEW", networkId)) {
          hbuilder.setNetworkId(networkId);
        }
      } else if (publishedNetworkService.findById(networkId) != null) {
        hbuilder.setNetworkId(networkId);
      }
    }

    if (dataset.getHarmonizationLink() != null) {
      hbuilder.setHarmonizationLink(createHarmonizationLinkDtoFromHarmonizationTable(dataset.getHarmonizationLink(), asDraft));
    }

    if(!dataset.getStudyTables().isEmpty()) {
      dataset.getStudyTables().forEach(studyTable -> hbuilder
        .addStudyTables(asDto(studyTable, true)));
    }

    if (!dataset.getHarmonizationTables().isEmpty()) {
      dataset.getHarmonizationTables().forEach(harmonizationTable -> hbuilder
        .addHarmonizationTables(asDto(harmonizationTable, true)));
    }

    if(!dataset.getNetworkTables().isEmpty()) {
      dataset.getNetworkTables().forEach(networkTable -> hbuilder
        .addNetworkTables(asDto(networkTable, true)));
    }

    builder.setExtension(Mica.HarmonizationDatasetDto.type, hbuilder.build());

    Mica.PermissionsDto permissionsDto = permissionsDtos.asDto(dataset);
    if(asDraft) {
      HarmonizationDatasetState state = harmonizationDatasetStateRepository.findOne(dataset.getId());
      if(state != null) {
        builder.setPublished(state.isPublished());
        builder.setExtension(Mica.EntityStateDto.datasetState,
          entityStateDtos.asDto(state).setPermissions(permissionsDto).build());
      }
    }

    builder.setPermissions(permissionsDto);

    return builder;
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull HarmonizationDataset dataset) {
    return asDto(dataset, false);
  }

  @NotNull
  Mica.DatasetDto asDto(@NotNull HarmonizationDataset dataset, boolean asDraft) {
    return asDtoBuilder(dataset, asDraft).build();
  }

  @NotNull
  Mica.DatasetVariableResolverDto.Builder asDto(@NotNull DatasetVariable.IdResolver resolver) {
    Mica.DatasetVariableResolverDto.Builder builder = Mica.DatasetVariableResolverDto.newBuilder();

    builder.setId(resolver.getId()) //
      .setDatasetId(resolver.getDatasetId()) //
      .setName(resolver.getName()) //
      .setVariableType(resolver.getType().name());

    if(resolver.hasStudyId()) {
      builder.setStudyId(resolver.getStudyId());
    }
    if(resolver.hasNetworkId()) {
      builder.setNetworkTableId(resolver.getNetworkId());
    }
    if(resolver.hasProject()) {
      builder.setProject(resolver.getProject());
    }
    if(resolver.hasTable()) {
      builder.setTable(resolver.getTable());
    }

    return builder;
  }

  @NotNull
  Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable) {
    return asDto(variable, Collections.emptyList(), "en");
  }

  @NotNull
  Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable, @NotNull List<Taxonomy> taxonomies, String locale) {
    Mica.DatasetVariableDto.Builder builder = Mica.DatasetVariableDto.newBuilder() //
      .setId(variable.getId()) //
      .setDatasetId(variable.getDatasetId()) //
      .addAllDatasetName(localizedStringDtos.asDto(variable.getDatasetName())) //
      .setName(variable.getName()) //
      .setEntityType(variable.getEntityType()) //
      .setValueType(variable.getValueType())//
      .setVariableType(variable.getVariableType().name()) //
      .setRepeatable(variable.isRepeatable()) //
      .setNature(variable.getNature()) //
      .setIndex(variable.getIndex());

    if(variable.getStudyIds() != null) {
      builder.addAllStudyIds(variable.getStudyIds());
      for(String studyId : variable.getStudyIds()) {
        builder.addStudySummaries(studySummaryDtos.asDto(studyId));
      }
    }

    if(variable.getNetworkTableIds() != null) {
      for(String networkId : variable.getNetworkTableIds()) {
        builder.addNetworkSummaries(networkSummaryDtos.asDto(networkId));
      }
    }

    if(!Strings.isNullOrEmpty(variable.getOccurrenceGroup())) {
      builder.setOccurrenceGroup(variable.getOccurrenceGroup());
    }

    if(!Strings.isNullOrEmpty(variable.getUnit())) {
      builder.setUnit(variable.getUnit());
    }

    if(!Strings.isNullOrEmpty(variable.getReferencedEntityType())) {
      builder.setReferencedEntityType(variable.getReferencedEntityType());
    }

    if(!Strings.isNullOrEmpty(variable.getMimeType())) {
      builder.setMimeType(variable.getMimeType());
    }

    if(variable.getAttributes() != null) {
      variable.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
      if(taxonomies != null) {
        taxonomies.forEach(taxonomy -> {
          Mica.TermAttributesDto dto = asDto(taxonomy, variable.getAttributes(), locale);
          if(dto.getVocabularyTermsCount() > 0) builder.addTermAttributes(dto);
        });
      }
    }

    if(variable.getCategories() != null) {
      variable.getCategories().forEach(category -> builder.addCategories(asDto(category)));
    }

    return builder.build();
  }

  @NotNull
  Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable, OpalTable opalTable) {
    Mica.DatasetVariableSummaryDto.Builder builder = Mica.DatasetVariableSummaryDto.newBuilder() //
      .setResolver(asDto(DatasetVariable.IdResolver.from(variable.getId())));

    if(variable.getAttributes() != null) {
      variable.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    if (opalTable instanceof StudyTable)
      builder.setStudyTable(asDto((StudyTable) opalTable));
    else if (opalTable instanceof NetworkTable)
      builder.setNetworkTable(asDto((NetworkTable) opalTable));

    return builder.build();
  }

  private Mica.TermAttributesDto asDto(Taxonomy taxonomy, Attributes attributes, String locale) {
    Mica.TermAttributesDto.Builder builder = Mica.TermAttributesDto.newBuilder() //
      .setTaxonomy(taxonomyDtos.asDto(taxonomy, locale));

    Map<String, Mica.TermAttributeDto.Builder> terms = Maps.newHashMap();
    attributes.getAttributes(taxonomy.getName()).forEach(attr -> {
      if(taxonomy.hasVocabulary(attr.getName())) {

        Vocabulary vocabulary = taxonomy.getVocabulary(attr.getName());
        String termStr = attr.getValues().getUndetermined();
        if(!Strings.isNullOrEmpty(termStr) && vocabulary.hasTerm(termStr)) {
          Mica.TermAttributeDto.Builder termBuilder;
          if(terms.containsKey(vocabulary.getName())) {
            termBuilder = terms.get(vocabulary.getName());
          } else {
            termBuilder = Mica.TermAttributeDto.newBuilder();
            terms.put(vocabulary.getName(), termBuilder);
            termBuilder.setVocabulary(taxonomyDtos.asDto(vocabulary, locale));
          }

          Term term = vocabulary.getTerm(termStr);
          termBuilder.addTerms(taxonomyDtos.asDto(term, locale));
        }
      }
    });

    // keep vocabulary order
    taxonomy.getVocabularies().forEach(vocabulary -> {
      if(terms.containsKey(vocabulary.getName())) {
        builder.addVocabularyTerms(terms.get(vocabulary.getName()));
      }
    });

    return builder.build();
  }

  private Mica.DatasetCategoryDto asDto(DatasetCategory category) {
    Mica.DatasetCategoryDto.Builder builder = Mica.DatasetCategoryDto.newBuilder() //
      .setName(category.getName()) //
      .setMissing(category.isMissing());

    if(category.getAttributes() != null) {
      category.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    return builder.build();
  }

  public Mica.DatasetDto.StudyTableDto.Builder asDto(StudyTable studyTable) {
    return asDto(studyTable, false);
  }

  public Mica.DatasetDto.StudyTableDto.Builder asDto(StudyTable studyTable, boolean includeSummary) {
    Mica.DatasetDto.StudyTableDto.Builder sbuilder = Mica.DatasetDto.StudyTableDto.newBuilder() //
      .setProject(studyTable.getProject())//
      .setTable(studyTable.getTable()) //
      .setWeight(studyTable.getWeight()) //
      .setStudyId(studyTable.getStudyId()) //
      .setPopulationId(studyTable.getPopulationId()) //
      .setDataCollectionEventId(studyTable.getDataCollectionEventId()) //
      .setDceId(studyTable.getDataCollectionEventUId());

    if(includeSummary) sbuilder.setStudySummary(studySummaryDtos.asDto(studyTable.getStudyId()));

    sbuilder.addAllName(localizedStringDtos.asDto(studyTable.getName()));
    sbuilder.addAllDescription(localizedStringDtos.asDto(studyTable.getDescription()));

    return sbuilder;
  }

  public Mica.DatasetDto.HarmonizationTableDto.Builder asDto(HarmonizationTable harmonizationTable) {
    return asDto(harmonizationTable, false);
  }

  public Mica.DatasetDto.HarmonizationTableDto.Builder asDto(HarmonizationTable harmonizationTable, boolean includeSummary) {
    Mica.DatasetDto.HarmonizationTableDto.Builder hBuilder = Mica.DatasetDto.HarmonizationTableDto.newBuilder()
      .setProject(harmonizationTable.getProject())
      .setTable(harmonizationTable.getTable())
      .setWeight(harmonizationTable.getWeight())
      .setStudyId(harmonizationTable.getStudyId())
      .setPopulationId(harmonizationTable.getPopulationId());

    if (includeSummary) hBuilder.setStudySummary(studySummaryDtos.asDto(harmonizationTable.getStudyId()));

    hBuilder.addAllName(localizedStringDtos.asDto(harmonizationTable.getName()));
    hBuilder.addAllDescription(localizedStringDtos.asDto(harmonizationTable.getDescription()));

    return hBuilder;
  }

  public Mica.DatasetVariableAggregationDto.Builder asDto(@NotNull OpalTable opalTable,
    @Nullable Math.SummaryStatisticsDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();

    if(summary == null) return aggDto;

    if(summary.hasExtension(Math.CategoricalSummaryDto.categorical)) {
      aggDto = asDto(summary.getExtension(Math.CategoricalSummaryDto.categorical));
    } else if(summary.hasExtension(Math.ContinuousSummaryDto.continuous)) {
      aggDto = asDto(summary.getExtension(Math.ContinuousSummaryDto.continuous));
    } else if(summary.hasExtension(Math.DefaultSummaryDto.defaultSummary)) {
      aggDto = asDto(summary.getExtension(Math.DefaultSummaryDto.defaultSummary));
    } else if(summary.hasExtension(Math.TextSummaryDto.textSummary)) {
      aggDto = asDto(summary.getExtension(Math.TextSummaryDto.textSummary));
    } else if(summary.hasExtension(Math.GeoSummaryDto.geoSummary)) {
      aggDto = asDto(summary.getExtension(Math.GeoSummaryDto.geoSummary));
    } else if(summary.hasExtension(Math.BinarySummaryDto.binarySummary)) {
      aggDto = asDto(summary.getExtension(Math.BinarySummaryDto.binarySummary));
    }

    if(opalTable instanceof StudyTable)
      aggDto.setStudyTable(asDto((StudyTable) opalTable));
    else if (opalTable instanceof NetworkTable)
      aggDto.setNetworkTable(asDto((NetworkTable) opalTable));


    return aggDto;
  }

  public Mica.DatasetDto.NetworkTableDto.Builder asDto(NetworkTable networkTable) {
    return asDto(networkTable, false);
  }

  public Mica.DatasetDto.NetworkTableDto.Builder asDto(NetworkTable networkTable, boolean includeSummary) {
    Mica.DatasetDto.NetworkTableDto.Builder sbuilder = Mica.DatasetDto.NetworkTableDto.newBuilder() //
      .setProject(networkTable.getProject())//
      .setTable(networkTable.getTable()) //
      .setWeight(networkTable.getWeight()) //
      .setNetworkId(networkTable.getNetworkId());

    if(includeSummary) sbuilder.setNetworkSummary(networkSummaryDtos.asDto(networkTable.getNetworkId(), false));

    sbuilder.addAllName(localizedStringDtos.asDto(networkTable.getName()));
    sbuilder.addAllDescription(localizedStringDtos.asDto(networkTable.getDescription()));

    return sbuilder;
  }

  public Mica.DatasetVariableContingencyDto.Builder asContingencyDto(@NotNull OpalTable opalTable,
    DatasetVariable variable, DatasetVariable crossVariable, @Nullable Search.QueryResultDto results) {
    Mica.DatasetVariableContingencyDto.Builder crossDto = Mica.DatasetVariableContingencyDto.newBuilder();

    if(opalTable instanceof StudyTable)
      crossDto.setStudyTable(asDto((StudyTable) opalTable, true));
    else if (opalTable instanceof NetworkTable)
      crossDto.setNetworkTable(asDto((NetworkTable) opalTable, true));

    Mica.DatasetVariableAggregationDto.Builder allAggBuilder = Mica.DatasetVariableAggregationDto.newBuilder();

    if(results == null) {
      allAggBuilder.setN(0);
      allAggBuilder.setTotal(0);
      crossDto.setAll(allAggBuilder);
      return crossDto;
    }

    allAggBuilder.setTotal(results.getTotalHits());
    MicaConfig micaConfig = micaConfigService.getConfig();
    int privacyThreshold = micaConfig.getPrivacyThreshold();
    crossDto.setPrivacyThreshold(privacyThreshold);
    boolean privacyChecks = crossVariable.hasCategories() ? validatePrivacyThreshold(results, privacyThreshold) : true;
    boolean totalPrivacyChecks = validateTotalPrivacyThreshold(results, privacyThreshold);

    // add facet results in the same order as the variable categories
    variable.getCategories().forEach(cat -> results.getFacetsList().stream()
      .filter(facet -> facet.hasFacet() && cat.getName().equals(facet.getFacet())).forEach(facet -> {
        boolean privacyCheck = privacyChecks && checkPrivacyThreshold(facet.getFilters(0).getCount(), privacyThreshold);
        Mica.DatasetVariableAggregationDto.Builder aggBuilder = Mica.DatasetVariableAggregationDto.newBuilder();
        aggBuilder.setTotal(totalPrivacyChecks ? results.getTotalHits() : 0);
        aggBuilder.setTerm(facet.getFacet());
        DatasetCategory category = variable.getCategory(facet.getFacet());
        aggBuilder.setMissing(category != null && category.isMissing());
        addSummaryStatistics(crossVariable, aggBuilder, facet, privacyCheck, totalPrivacyChecks);
        crossDto.addAggregations(aggBuilder);
      }));

    // add total facet for all variable categories
    results.getFacetsList().stream().filter(facet -> facet.hasFacet() && "_total".equals(facet.getFacet()))
      .forEach(facet -> {
        boolean privacyCheck = privacyChecks && facet.getFilters(0).getCount() > micaConfig.getPrivacyThreshold();
        addSummaryStatistics(crossVariable, allAggBuilder, facet, privacyCheck, totalPrivacyChecks);
      });

    crossDto.setAll(allAggBuilder);

    return crossDto;
  }

  private boolean checkPrivacyThreshold(int count, int threshold) {
    return count == 0 || count >= threshold;
  }

  private boolean validateTotalPrivacyThreshold(Search.QueryResultDtoOrBuilder results, int privacyThreshold) {
    return results.getFacetsList().stream()
      .allMatch(facet -> checkPrivacyThreshold(facet.getFilters(0).getCount(), privacyThreshold));
  }

  private boolean validatePrivacyThreshold(Search.QueryResultDtoOrBuilder results, int privacyThreshold) {
    return results.getFacetsList().stream().map(Search.FacetResultDto::getFrequenciesList).flatMap(Collection::stream)
      .allMatch(freq -> checkPrivacyThreshold(freq.getCount(), privacyThreshold));
  }

  private void addSummaryStatistics(DatasetVariable crossVariable,
    Mica.DatasetVariableAggregationDto.Builder aggBuilder, Search.FacetResultDto facet, boolean privacyCheck,
    boolean totalPrivacyCheck) {
    aggBuilder.setN(totalPrivacyCheck ? facet.getFilters(0).getCount() : -1);
    if(!privacyCheck) return;

    if(crossVariable.hasCategories()) {
      // order results as the order of cross variable categories
      crossVariable.getCategories().forEach(
        cat -> facet.getFrequenciesList().stream().filter(freq -> cat.getName().equals(freq.getTerm()))
          .forEach(freq -> aggBuilder.addFrequencies(asDto(crossVariable, freq))));
    }

    if(facet.hasStatistics()) {
      aggBuilder.setStatistics(asDto(facet.getStatistics()));
    }
  }

  private Mica.FrequencyDto.Builder asDto(DatasetVariable crossVariable,
    Search.FacetResultDto.TermFrequencyResultDto result) {
    DatasetCategory category = crossVariable.getCategory(result.getTerm());
    return Mica.FrequencyDto.newBuilder() //
      .setValue(result.getTerm()) //
      .setCount(result.getCount()) //
      .setMissing(category != null && category.isMissing());
  }

  private Mica.StatisticsDto.Builder asDto(Search.FacetResultDto.StatisticalResultDto result) {
    return Mica.StatisticsDto.newBuilder() //
      .setMin(result.getMin()) //
      .setMax(result.getMax()) //
      .setMean(result.getMean()) //
      .setSum(result.getTotal()) //
      .setSumOfSquares(result.getSumOfSquares()) //
      .setVariance(result.getVariance()) //
      .setStdDeviation(result.getStdDeviation());
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.CategoricalSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList(),
      summary.hasOtherFrequency() ? Long.valueOf(summary.getOtherFrequency()).intValue() : 0);
    return aggDto;
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.DefaultSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.TextSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList(),
      summary.hasOtherFrequency() ? Long.valueOf(summary.getOtherFrequency()).intValue() : 0);
    return aggDto;
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.GeoSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.BinarySummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private Mica.FrequencyDto.Builder asDto(Math.FrequencyDto freq) {
    return Mica.FrequencyDto.newBuilder().setValue(freq.getValue()).setCount(Long.valueOf(freq.getFreq()).intValue())
      .setMissing(freq.getMissing());
  }

  private void addFrequenciesDto(Mica.DatasetVariableAggregationDto.Builder aggDto,
    List<Math.FrequencyDto> frequencies) {
    addFrequenciesDto(aggDto, frequencies, 0);
  }

  private void addFrequenciesDto(Mica.DatasetVariableAggregationDto.Builder aggDto, List<Math.FrequencyDto> frequencies,
    int otherFrequency) {
    int n = otherFrequency;
    if(frequencies != null) {
      for(Math.FrequencyDto freq : frequencies) {
        aggDto.addFrequencies(asDto(freq));
        if(!freq.getMissing()) n += freq.getFreq();
      }
    }
    aggDto.setN(n);
  }

  private Mica.DatasetVariableAggregationDto.Builder asDto(Math.ContinuousSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    Math.DescriptiveStatsDto stats = summary.getSummary();

    aggDto.setN(Long.valueOf(stats.getN()).intValue());

    Mica.StatisticsDto.Builder builder = Mica.StatisticsDto.newBuilder();

    if(stats.hasSum()) builder.setSum(Double.valueOf(stats.getSum()).floatValue());
    if(stats.hasMin() && stats.getMin() != Double.POSITIVE_INFINITY)
      builder.setMin(Double.valueOf(stats.getMin()).floatValue());
    if(stats.hasMax() && stats.getMax() != Double.NEGATIVE_INFINITY)
      builder.setMax(Double.valueOf(stats.getMax()).floatValue());
    if(stats.hasMean() && !Double.isNaN(stats.getMean())) builder.setMean(Double.valueOf(stats.getMean()).floatValue());
    if(stats.hasSumsq() && !Double.isNaN(stats.getSumsq()))
      builder.setSumOfSquares(Double.valueOf(stats.getSumsq()).floatValue());
    if(stats.hasVariance() && !Double.isNaN(stats.getVariance()))
      builder.setVariance(Double.valueOf(stats.getVariance()).floatValue());
    if(stats.hasStdDev() && !Double.isNaN(stats.getStdDev()))
      builder.setStdDeviation(Double.valueOf(stats.getStdDev()).floatValue());

    aggDto.setStatistics(builder);

    if(summary.getFrequenciesCount() > 0) {
      summary.getFrequenciesList().forEach(freq -> aggDto.addFrequencies(asDto(freq)));
    }

    int total = 0;
    if(summary.getFrequenciesCount() > 0) {
      for(Math.FrequencyDto freq : summary.getFrequenciesList()) {
        total += freq.getFreq();
      }
    }
    aggDto.setTotal(total);

    return aggDto;
  }

  private Mica.DatasetDto.Builder asBuilder(Dataset dataset) {
    Mica.DatasetDto.Builder builder = Mica.DatasetDto.newBuilder();
    if(dataset.getId() != null) builder.setId(dataset.getId());
    builder.setTimestamps(TimestampsDtos.asDto(dataset)) //
      .setEntityType(dataset.getEntityType()) //
      .addAllName(localizedStringDtos.asDto(dataset.getName())) //
      .addAllAcronym(localizedStringDtos.asDto(dataset.getAcronym())) //
      .addAllDescription(localizedStringDtos.asDto(dataset.getDescription()));

    if(dataset.getAttributes() != null) {
      dataset.getAttributes().asAttributeList()
        .forEach(attribute -> builder.addAttributes(attributeDtos.asDto(attribute)));
    }

    if (dataset.hasModel()) {
      builder.setContent(JSONUtils.toJSON(dataset.getModel()));
    }

    return builder;
  }

  @NotNull
  public Dataset fromDto(Mica.DatasetDtoOrBuilder dto) {
    Dataset dataset = dto.hasExtension(Mica.HarmonizationDatasetDto.type)
      ? fromDto(dto.getExtension(Mica.HarmonizationDatasetDto.type))
      : dto.hasExtension(Mica.CollectionDatasetDto.type)
        ? fromDto(dto.getExtension(Mica.CollectionDatasetDto.type))
        : new StudyDataset();

    if(dto.hasId()) dataset.setId(dto.getId());
    dataset.setAcronym(localizedStringDtos.fromDto(dto.getAcronymList()));
    dataset.setName(localizedStringDtos.fromDto(dto.getNameList()));
    dataset.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));
    dataset.setEntityType(dto.getEntityType());
    dataset.setPublished(dto.getPublished());
    if(dto.getAttributesCount() > 0) {
      dto.getAttributesList().forEach(attributeDto -> dataset.addAttribute(attributeDtos.fromDto(attributeDto)));
    }
    if (dto.hasContent()) {
      dataset.setModel(JSONUtils.toMap(dto.getContent()));
    }

    return dataset;
  }

  private Dataset fromDto(@NotNull Mica.HarmonizationDatasetDto dto) {
    Assert.notNull(dto, "HarmonizationDataset dt cannot be null.");
    HarmonizationDataset harmonizationDataset = new HarmonizationDataset();

    if(dto.getStudyTablesCount() > 0) {
      dto.getStudyTablesList().forEach(tableDto -> harmonizationDataset.addStudyTable(fromDto(tableDto)));
    }

    if (dto.getHarmonizationTablesCount() > 0) {
      dto.getHarmonizationTablesList().forEach(tableDto -> harmonizationDataset.addHarmonizationTable(fromDto(tableDto)));
    }

    if(dto.getNetworkTablesCount() > 0) {
      dto.getNetworkTablesList().forEach(tableDto -> harmonizationDataset.addNetworkTable(fromDto(tableDto)));
    }

    String networkId = dto.getNetworkId();
    harmonizationDataset.setNetworkId(Strings.isNullOrEmpty(networkId) ? null : networkId);
    if (dto.hasHarmonizationLink()) {
      HarmonizationTable harmonizationLink = new HarmonizationTable();
      harmonizationLink.setProject(dto.getHarmonizationLink().getProject());
      harmonizationLink.setTable(dto.getHarmonizationLink().getTable());
      harmonizationLink.setStudyId(dto.getHarmonizationLink().getStudyId());
      harmonizationLink.setPopulationId(dto.getHarmonizationLink().getPopulationId());

      harmonizationDataset.setHarmonizationLink(harmonizationLink);
    }
    return harmonizationDataset;
  }

  private Dataset fromDto(@NotNull Mica.CollectionDatasetDto dto) {
    Assert.notNull(dto, "StudyDataset dt cannot be null.");
    StudyDataset studyDataset = new StudyDataset();
    Optional.ofNullable(dto).ifPresent(ext -> studyDataset.setStudyTable(fromDto(ext.getStudyTable())));
    return studyDataset;
  }

  private StudyTable fromDto(Mica.DatasetDto.StudyTableDto dto) {
    StudyTable table = new StudyTable();
    table.setStudyId(dto.getStudyId());
    table.setPopulationId(dto.getPopulationId());
    table.setDataCollectionEventId(dto.getDataCollectionEventId());
    table.setProject(dto.getProject());
    table.setTable(dto.getTable());
    table.setWeight(dto.getWeight());

    table.setName(localizedStringDtos.fromDto(dto.getNameList()));
    table.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));

    return table;
  }

  private HarmonizationTable fromDto(Mica.DatasetDto.HarmonizationTableDto dto) {
    HarmonizationTable table = new HarmonizationTable();
    table.setStudyId(dto.getStudyId());
    table.setPopulationId(dto.getPopulationId());
    table.setPopulationId(dto.getPopulationId());
    table.setProject(dto.getProject());
    table.setTable(dto.getTable());
    table.setWeight(dto.getWeight());

    table.setName(localizedStringDtos.fromDto(dto.getNameList()));
    table.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));

    return table;
  }

  private NetworkTable fromDto(Mica.DatasetDto.NetworkTableDto dto) {
    NetworkTable table = new NetworkTable();
    table.setNetworkId(dto.getNetworkId());
    table.setProject(dto.getProject());
    table.setTable(dto.getTable());
    table.setWeight(dto.getWeight());

    table.setName(localizedStringDtos.fromDto(dto.getNameList()));
    table.setDescription(localizedStringDtos.fromDto(dto.getDescriptionList()));

    return table;
  }

  private Mica.DatasetDto.HarmonizationTableDto.Builder createHarmonizationLinkDtoFromHarmonizationTable(HarmonizationTable harmonizationLink, boolean asDraft) {
    Mica.DatasetDto.HarmonizationTableDto.Builder harmonizationLinkBuilder = Mica.DatasetDto.HarmonizationTableDto.newBuilder();

    if (!Strings.isNullOrEmpty(harmonizationLink.getProject()))
      harmonizationLinkBuilder.setProject(harmonizationLink.getProject());

    if (!Strings.isNullOrEmpty(harmonizationLink.getTable()))
      harmonizationLinkBuilder.setTable(harmonizationLink.getTable());

    String studyId = harmonizationLink.getStudyId();

    if (asDraft) {
      if(studyId != null && subjectAclService.isPermitted("/draft/harmonization-study", "VIEW", studyId)) {
        harmonizationLinkBuilder.setStudyId(harmonizationLink.getStudyId());
        harmonizationLinkBuilder.setPopulationId(harmonizationLink.getPopulationId());
      }
    } else if (studyId != null && publishedStudyService.findById(studyId) != null) {
      harmonizationLinkBuilder.setStudyId(harmonizationLink.getStudyId());
      harmonizationLinkBuilder.setPopulationId(harmonizationLink.getPopulationId());
    }

    if (studyId != null) harmonizationLinkBuilder.setStudySummary(studySummaryDtos.asHarmoStudyDto(studyId));

    return harmonizationLinkBuilder;
  }
}
