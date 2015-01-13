/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.obiba.mica.search.queries.AbstractDocumentQuery;
import org.obiba.mica.search.queries.AbstractDocumentQuery.Mode;
import org.obiba.mica.search.queries.DatasetQuery;
import org.obiba.mica.search.queries.NetworkQuery;
import org.obiba.mica.search.queries.StudyQuery;
import org.obiba.mica.search.queries.VariableQuery;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.obiba.mica.search.queries.AbstractDocumentQuery.Scope.DETAIL;
import static org.obiba.mica.search.queries.AbstractDocumentQuery.Scope.DIGEST;
import static org.obiba.mica.web.model.MicaSearch.JoinQueryDto;
import static org.obiba.mica.web.model.MicaSearch.JoinQueryResultDto;

@Component
@Scope("request")
public class JoinQueryExecutor {

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  public enum QueryType {
    VARIABLE,
    DATASET,
    STUDY,
    NETWORK
  }

  @Inject
  private VariableQuery variableQuery;

  @Inject
  private DatasetQuery datasetQuery;

  @Inject
  private StudyQuery studyQuery;

  @Inject
  private NetworkQuery networkQuery;

  public JoinQueryResultDto queryCoverage(QueryType type, JoinQueryDto joinQueryDto) throws IOException {
    return query(type, joinQueryDto, null, DIGEST, Mode.COVERAGE);
  }

  public JoinQueryResultDto listQuery(QueryType type, MicaSearch.QueryDto queryDto , String locale) throws IOException {
    JoinQueryDto joinQueryDto = createJoinQueryByType(type, queryDto);
    variableQuery.initialize(joinQueryDto.hasVariableQueryDto() ? joinQueryDto.getVariableQueryDto() : null, locale, Mode.SEARCH);
    datasetQuery.initialize(joinQueryDto.hasDatasetQueryDto() ? joinQueryDto.getDatasetQueryDto() : null, locale, Mode.SEARCH);
    studyQuery.initialize(joinQueryDto.hasStudyQueryDto() ? joinQueryDto.getStudyQueryDto() : null, locale, Mode.SEARCH);
    networkQuery.initialize(joinQueryDto.hasNetworkQueryDto() ? joinQueryDto.getNetworkQueryDto() : null, locale, Mode.SEARCH);

    execute(type, DETAIL, CountStatsData.newBuilder());

    JoinQueryResultDto.Builder builder = JoinQueryResultDto.newBuilder();
    if(variableQuery.getResultQuery() != null) builder.setVariableResultDto(variableQuery.getResultQuery());
    if(datasetQuery.getResultQuery() != null) builder.setDatasetResultDto(datasetQuery.getResultQuery());
    if(studyQuery.getResultQuery() != null) builder.setStudyResultDto(studyQuery.getResultQuery());
    if(networkQuery.getResultQuery() != null) builder.setNetworkResultDto(networkQuery.getResultQuery());

    return builder.build();
  }

  private JoinQueryDto createJoinQueryByType(QueryType type, MicaSearch.QueryDto queryDto) {
    JoinQueryDto.Builder builder = JoinQueryDto.newBuilder();

    switch(type) {
      case VARIABLE:
        builder.setVariableQueryDto(queryDto).build();
        break;
      case DATASET:
        builder.setDatasetQueryDto(queryDto).build();
        break;
      case STUDY:
        builder.setStudyQueryDto(queryDto).build();
        break;
      case NETWORK:
        builder.setNetworkQueryDto(queryDto);
        break;
    }

    return builder.build();
  }

  public JoinQueryResultDto query(QueryType type, JoinQueryDto joinQueryDto) throws IOException {
    return query(type, joinQueryDto, CountStatsData.newBuilder(), DETAIL, Mode.SEARCH);
  }

  private JoinQueryResultDto query(QueryType type, JoinQueryDto joinQueryDto, CountStatsData.Builder countBuilder,
      AbstractDocumentQuery.Scope scope, AbstractDocumentQuery.Mode mode) throws IOException {
    DatasetIdProvider datasetIdProvider = new DatasetIdProvider();
    String locale = joinQueryDto.getLocale();

    variableQuery.initialize(joinQueryDto.hasVariableQueryDto() ? joinQueryDto.getVariableQueryDto() : null, locale, mode);
    datasetQuery.initialize(joinQueryDto.hasDatasetQueryDto() ? joinQueryDto.getDatasetQueryDto() : null, locale, mode);
    studyQuery.initialize(joinQueryDto.hasStudyQueryDto() ? joinQueryDto.getStudyQueryDto() : null, locale, mode);
    networkQuery.initialize(joinQueryDto.hasNetworkQueryDto() ? joinQueryDto.getNetworkQueryDto() : null, locale, mode);

    boolean queriesHaveFilters = Stream.of(variableQuery, datasetQuery, studyQuery, networkQuery)
        .filter(AbstractDocumentQuery::hasQueryFilters).collect(Collectors.toList()).size() > 0;

    if (queriesHaveFilters) {
      variableQuery.setDatasetIdProvider(datasetIdProvider);
      datasetQuery.setDatasetIdProvider(datasetIdProvider);
      List<String> joinedIds = executeJoin(type);

      CountStatsData countStats = countBuilder != null ? getCountStatsData(type) : null;

      if (joinedIds != null && joinedIds.size() > 0) {
        getDocumentQuery(type).query(joinedIds, countStats, scope);
        // need to update dataset and variable and redo agg query
        if(type == QueryType.VARIABLE) {
          datasetQuery.query(joinedIds, null, DIGEST);
        } else if(type == QueryType.DATASET) {
          variableQuery.query(joinedIds, null, DIGEST);
        }
      } else if(!studyQuery.hasQueryFilters()) {
        variableQuery.query(joinedIds, null, DIGEST); //to set datasetprovider datasets if any

        if(!datasetIdProvider.getDatasetIds().isEmpty()) {
          if(type != QueryType.DATASET) {
            datasetQuery.query(joinedIds, null, DIGEST);
          }

          if(type == QueryType.VARIABLE || type == QueryType.DATASET) {
            getDocumentQuery(type).query(joinedIds, countStats, scope);
          }
        }

        List<String> tmp = new ArrayList<>();
        tmp.add(""); //fake study id to get only aggregations summary
        studyQuery.query(tmp, null, DIGEST);
      }
    } else {
      execute(type, scope, countBuilder);
    }

    JoinQueryResultDto.Builder builder = JoinQueryResultDto.newBuilder();
    if(variableQuery.getResultQuery() != null) builder.setVariableResultDto(variableQuery.getResultQuery());
    if(datasetQuery.getResultQuery() != null) builder.setDatasetResultDto(datasetQuery.getResultQuery());
    if(studyQuery.getResultQuery() != null) builder.setStudyResultDto(studyQuery.getResultQuery());
    if(networkQuery.getResultQuery() != null) builder.setNetworkResultDto(networkQuery.getResultQuery());

    return builder.build();
  }

  private void execute(QueryType type, AbstractDocumentQuery.Scope scope, CountStatsData.Builder countBuilder)
      throws IOException {

    CountStatsData countStats;

    switch(type) {
      case VARIABLE:
        queryAggregations(null, studyQuery, datasetQuery, networkQuery);
        countStats = countBuilder != null ? getCountStatsData(type) : null;
        variableQuery.query(null, countStats, scope);
        break;
      case DATASET:
        queryAggregations(null, variableQuery, studyQuery, networkQuery);
        countStats = countBuilder != null ? getCountStatsData(type) : null;
        datasetQuery.query(null, countStats, scope);
        break;
      case STUDY:
        queryAggregations(null, variableQuery, datasetQuery, networkQuery);
        countStats = countBuilder != null ? getCountStatsData(type) : null;
        studyQuery.query(null, countStats, scope);
        break;
      case NETWORK:
        queryAggregations(null, variableQuery, datasetQuery, studyQuery);
        countStats = countBuilder != null ? getCountStatsData(type) : null;
        networkQuery.query(null, countStats, scope);
        break;
    }
  }

  private List<String> executeJoin(QueryType type) throws IOException {
    List<String> joinedIds = null;

    switch(type) {
      case VARIABLE:
        joinedIds = execute(variableQuery, studyQuery, datasetQuery, networkQuery);
        break;
      case DATASET:
        joinedIds = execute(datasetQuery, variableQuery, studyQuery, networkQuery);
        break;
      case STUDY:
        joinedIds = execute(studyQuery, variableQuery, datasetQuery, networkQuery);
        break;
      case NETWORK:
        joinedIds = execute(networkQuery, variableQuery, datasetQuery, studyQuery);
        break;
    }

    return joinedIds;
  }

  private AbstractDocumentQuery getDocumentQuery(QueryType type) {
    switch(type) {
      case VARIABLE:
        return variableQuery;
      case DATASET:
        return datasetQuery;
      case STUDY:
        return studyQuery;
      case NETWORK:
        return networkQuery;
    }

    throw new IllegalArgumentException("Illegal query type: " + type);
  }

  private CountStatsData getCountStatsData(QueryType type) {
    CountStatsData countStats = null;
    switch(type) {
      case DATASET:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getDatasetCounts())
            .studies(studyQuery.getStudyCounts()).networksMap(networkQuery.getStudyCountsByNetwork()).build();
        break;
      case STUDY:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getStudyCounts())
            .studyDatasets(datasetQuery.getStudyCounts())
            .harmonizationDatasets(datasetQuery.getHarmonizationStudyCounts()).networks(networkQuery.getStudyCounts())
            .build();
        break;
      case NETWORK:
        countStats = CountStatsData.newBuilder().variables(variableQuery.getStudyCounts())
            .datasetsMap(datasetQuery.getStudyCountsByDataset())
            .studies(studyQuery.getStudyCounts())
            .build();
        break;
    }

    return countStats;
  }

  private List<String> execute(AbstractDocumentQuery docQuery, AbstractDocumentQuery... subQueries) throws IOException {
    List<AbstractDocumentQuery> queries = Arrays.asList(subQueries).stream()
        .filter(AbstractDocumentQuery::hasQueryFilters).collect(Collectors.toList());

    List<String> studyIds = null;
    List<String> docQueryStudyIds = null;
    if(queries.size() > 0) studyIds = queryStudyIds(queries);
    if(studyIds == null || studyIds.size() > 0) docQueryStudyIds = docQuery.queryStudyIds(studyIds);

    List<String> aggStudyIds =
        docQuery.hasQueryFilters() && docQueryStudyIds != null
            ? joinStudyIds(studyIds, docQueryStudyIds)
            : studyIds;

    if(aggStudyIds == null || aggStudyIds.size() > 0) {
      queryAggregations(aggStudyIds, subQueries);
    }

    return aggStudyIds;
  }

  private List<String> joinStudyIds(List<String> studyIds, List<String> joinedStudyIds) {
    if (studyIds != null) {
      joinedStudyIds.retainAll(studyIds);
    }

    return joinedStudyIds;
  }

  private void queryAggregations(List<String> studyIds, AbstractDocumentQuery... queries) throws IOException {
    for(AbstractDocumentQuery query : queries) query.query(studyIds, null, DIGEST);
  }

  private List<String> queryStudyIds(List<AbstractDocumentQuery> queries) throws IOException {
    List<String> studyIds = queries.get(0).queryStudyIds();
    queries.subList(1, queries.size()).forEach(query -> {
      if(studyIds.size() > 0) {
        try {
          studyIds.retainAll(query.queryStudyIds());
        } catch(IOException e) {
          log.error("Failed to query study IDs '{}'", e);
        }
        if(studyIds.isEmpty()) return;
      }
    });

    return studyIds;
  }

}
