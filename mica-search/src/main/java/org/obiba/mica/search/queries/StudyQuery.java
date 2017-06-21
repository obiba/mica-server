/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.core.domain.DefaultEntityBase;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.aggregations.StudyTaxonomyMetaDataProvider;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.search.StudyIndexer;
import org.obiba.mica.study.service.CollectionStudyService;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.obiba.mica.search.CountStatsDtoBuilders.StudyCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;
import static org.obiba.mica.web.model.MicaSearch.StudyResultDto;

@Component
@Scope("request")
public class StudyQuery extends AbstractDocumentQuery {

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private CollectionStudyService collectionStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Inject
  private Dtos dtos;

  @Inject
  private StudyTaxonomyMetaDataProvider studyTaxonomyMetaDataProvider;

  private static final String JOIN_FIELD = "id";

  @Override
  public String getSearchIndex() {
    return StudyIndexer.PUBLISHED_STUDY_INDEX;
  }

  @Override
  public String getSearchType() {
    return StudyIndexer.STUDY_TYPE;
  }

  @Override
  public QueryBuilder getAccessFilter() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = collectionStudyService.findPublishedStates().stream().map(DefaultEntityBase::getId)
      .filter(s -> subjectAclService.isAccessible("/collection-study", s)).collect(Collectors.toList());
    ids.addAll(harmonizationStudyService.findPublishedStates().stream().map(DefaultEntityBase::getId)
      .filter(s -> subjectAclService.isAccessible("/harmonization-study", s)).collect(Collectors.toList()));
    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().ids(ids.toArray(new String[ids.size()]));
  }

  @Override
  public Stream<String> getLocalizedQueryStringFields() {
    return Stream.of(StudyIndexer.LOCALIZED_ANALYZED_FIELDS);
  }

  @Override
  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Arrays.asList(studyTaxonomyMetaDataProvider);
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope, CountStatsData counts) {
    StudyResultDto.Builder resBuilder = StudyResultDto.newBuilder();
    StudyCountStatsBuilder studyCountStatsBuilder = counts == null ? null : StudyCountStatsBuilder.newBuilder(counts);

    Consumer<BaseStudy> addDto = getStudyConsumer(scope, resBuilder, studyCountStatsBuilder);
    List<String> hitsIds = Stream.of(hits.hits()).map(h -> h.getId()).collect(Collectors.toList());
    List<BaseStudy> publishedStudies = publishedStudyService.findByIds(hitsIds);
    publishedStudies.forEach(addDto);
    builder.setExtension(StudyResultDto.result, resBuilder.build());
  }

  private Consumer<BaseStudy> getStudyConsumer(Scope scope, StudyResultDto.Builder resBuilder,
    StudyCountStatsBuilder studyCountStatsBuilder) {

    return scope == Scope.DETAIL ? (study) -> {
      Mica.StudySummaryDto.Builder summaryBuilder = dtos.asSummaryDtoBuilder(study);
      if(mode == Mode.LIST) {
        summaryBuilder.clearPopulationSummaries();
      }
      if(studyCountStatsBuilder != null) {
        summaryBuilder.setExtension(MicaSearch.CountStatsDto.studyCountStats, studyCountStatsBuilder.build(study))
          .build();
      }
      resBuilder.addSummaries(summaryBuilder.build());
    } : (study) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(study).build());
  }

  @Nullable
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    Properties properties = getAggregationsProperties(filter, taxonomyService.getStudyTaxonomy());
    if(!properties.containsKey(JOIN_FIELD)) properties.put(JOIN_FIELD,"");
    return properties;
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(JOIN_FIELD);
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }
}
