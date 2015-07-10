/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.study;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.dataset.search.rest.harmonized.CsvContingencyWriter;
import org.obiba.mica.dataset.search.rest.harmonized.ExcelContingencyWriter;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
@Scope("request")
@RequiresAuthentication
public class PublishedStudyDatasetVariableResource extends AbstractPublishedDatasetResource<StudyDataset>
  implements DatasetVariableResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedStudyDatasetVariableResource.class);

  private String datasetId;

  private String variableName;

  @Inject
  private StudyDatasetService datasetService;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return getDatasetVariableDto(datasetId, variableName, DatasetVariable.Type.Study);
  }

  @GET
  @Path("/summary")
  public org.obiba.opal.web.model.Math.SummaryStatisticsDto getVariableSummary() {
    return datasetService.getVariableSummary(getDataset(StudyDataset.class, datasetId), variableName).getWrappedDto();
  }

  @GET
  @Path("/facet")
  public Search.QueryResultDto getVariableFacet() {
    return datasetService.getVariableFacet(getDataset(StudyDataset.class, datasetId), variableName);
  }

  @GET
  @Path("/aggregation")
  public Mica.DatasetVariableAggregationDto getVariableAggregations() {
    StudyDataset dataset = getDataset(StudyDataset.class, datasetId);
    StudyTable studyTable = dataset.getStudyTable();
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder() //
      .setStudyTable(dtos.asDto(studyTable));
    try {
      return dtos.asDto(studyTable, datasetService.getVariableSummary(dataset, variableName).getWrappedDto()).build();
    } catch(Exception e) {
      log.warn("Unable to retrieve statistics: " + e.getMessage(), e);
      return dtos.asDto(studyTable, null).build();
    }
  }

  @GET
  @Path("/contingency")
  public Mica.DatasetVariableContingencyDto getContingency(@QueryParam("by") String crossVariable) {
    return getContingencyDto(crossVariable);
  }

  private Mica.DatasetVariableContingencyDto getContingencyDto(String crossVariable) {
    if(Strings.isNullOrEmpty(crossVariable))
      throw new BadRequestException("Cross variable name is required for the contingency table");

    StudyDataset dataset = getDataset(StudyDataset.class, datasetId);
    StudyTable studyTable = dataset.getStudyTable();
    DatasetVariable var = getDatasetVariable(datasetId, variableName, DatasetVariable.Type.Study, null);
    DatasetVariable crossVar = getDatasetVariable(datasetId, crossVariable, DatasetVariable.Type.Study, null);

    try {
      return dtos.asContingencyDto(studyTable, var, crossVar, datasetService.getContingencyTable(dataset, var, crossVar)).build();
    } catch(Exception e) {
      log.warn("Unable to retrieve contingency table: " + e.getMessage(), e);
      return dtos.asContingencyDto(studyTable, var, crossVar, null).build();
    }
  }

  @GET
  @Path("/contingency/_export")
  @Produces("text/csv")
  public Response getContingencyCsv(@QueryParam("by") String crossVariable) throws IOException {
    ByteArrayOutputStream res = new CsvContingencyWriter().write(getContingencyDto(crossVariable));

    return Response.ok(res.toByteArray()).header("Content-Disposition",
      String.format("attachment; filename=\"contingency-table-%s-%s.csv\"", variableName, crossVariable)).build();
  }

  @GET
  @Path("/contingency/_export")
  @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public Response getContingencyExcel(@QueryParam("by") String crossVariable) throws IOException {
    ByteArrayOutputStream res = new ExcelContingencyWriter(variableName, crossVariable).write(getContingencyDto(crossVariable));

    return Response.ok(res.toByteArray()).header("Content-Disposition",
      String.format("attachment; filename=\"contingency-table-%s-%s.xlsx\"", variableName, crossVariable)).build();
  }

  @Override
  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  @Override
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }
}
