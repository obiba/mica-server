/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;

@Component
@Scope("request")
@Path("/harmonization-dataset/{id}")
@RequiresAuthentication
public class PublishedHarmonizationDatasetResource extends AbstractPublishedDatasetResource<HarmonizationDataset> {

  private static final Logger log = LoggerFactory.getLogger(PublishedHarmonizationDatasetResource.class);

  @Inject
  private SubjectAclService subjectAclService;

  /**
   * Get {@link HarmonizationDataset} from published index.
   *
   * @return
   */
  @GET
  @Timed
  public Mica.DatasetDto get(@PathParam("id") String id) {
    checkAccess(id);
    return getDatasetDto(HarmonizationDataset.class, id);
  }

  /**
   * Get the {@link DatasetVariable}s from published index.
   *
   * @param queryString - Elasticsearch query string 'field1: value AND field2: value'
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  @Path("/variables/_search")
  @Timed
  public Mica.DatasetVariablesDto queryVariables(@PathParam("id") String id, @QueryParam("query") String queryString,
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
    @QueryParam("sort") String sort, @QueryParam("order") String order) {
    checkAccess(id);
    return getDatasetVariableDtos(queryString, id, DatasetVariable.Type.Dataschema, from, limit, sort, order);
  }

  /**
   * Get the {@link DatasetVariable}s from published index.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  @Path("/variables")
  @Timed
  public Mica.DatasetVariablesDto getVariables(@PathParam("id") String id,
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
    @QueryParam("sort") String sort, @QueryParam("order") String order) {
    checkAccess(id);
    return getDatasetVariableDtos(id, DatasetVariable.Type.Dataschema, from, limit, sort, order);
  }

  /**
   * Get the harmonized variable summaries for each of the queried dataschema variables and for each of the study.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  @Path("/variables/harmonizations")
  @Timed
  public Mica.DatasetVariablesHarmonizationsDto getVariableHarmonizations(@PathParam("id") String id,
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
    @QueryParam("sort") @DefaultValue("index") String sort, @QueryParam("order") @DefaultValue("asc") String order) {
    checkAccess(id);
    Mica.DatasetVariablesHarmonizationsDto.Builder builder = Mica.DatasetVariablesHarmonizationsDto.newBuilder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    Mica.DatasetVariablesDto variablesDto = getDatasetVariableDtos(id, DatasetVariable.Type.Dataschema, from, limit,
      sort, order);

    builder.setTotal(variablesDto.getTotal()).setLimit(variablesDto.getLimit()).setFrom(variablesDto.getFrom());

    variablesDto.getVariablesList()
      .forEach(variable -> builder.addVariableHarmonizations(getVariableHarmonizationDto(dataset, variable.getName())));

    return builder.build();
  }

  @GET
  @Path("/variables/harmonizations/_export")
  @Produces("text/csv")
  @Timed
  public Response getVariableHarmonizationsAsCsv(@PathParam("id") String id,
    @QueryParam("sort") @DefaultValue("index") String sort, @QueryParam("order") @DefaultValue("asc") String order,
    @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {
    checkAccess(id);
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    Mica.DatasetVariablesHarmonizationsDto harmonizationVariables = getVariableHarmonizations(id, 0, 999999, sort, order);

    CsvHarmonizationVariablesWriter writer = new CsvHarmonizationVariablesWriter(
      Lists.newArrayList("maelstrom", "Mlstr_harmo"));
    ByteArrayOutputStream values = writer.write(dataset, harmonizationVariables, locale);

    return Response.ok(values.toByteArray(), "text/csv")
      .header("Content-Disposition", "attachment; filename=\"" + id + ".csv\"").build();
  }

  @Path("/variable/{variable}")
  public PublishedDataschemaDatasetVariableResource getVariable(@PathParam("id") String id,
    @PathParam("variable") String variable) {
    checkAccess(id);
    PublishedDataschemaDatasetVariableResource resource = applicationContext
      .getBean(PublishedDataschemaDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  @GET
  @Path("/study/{study}/variables")
  @Timed
  public Mica.DatasetVariablesDto getVariables(@PathParam("id") String id, @PathParam("study") String studyId,
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
    @QueryParam("sort") String sort, @QueryParam("order") String order) {
    checkAccess(id);
    QueryBuilder query = FilteredQueryBuilder.newBuilder().must("datasetId", id).must("studyId", studyId)
      .must("variableType", DatasetVariable.Type.Dataschema.toString())
      .build(QueryBuilders.matchAllQuery());

    return getDatasetVariableDtosInternal(query, from, limit, sort, order);
  }

  @Path("/study/{study}/population/{population}/data-collection-event/{dce}/variable/{variable}")
  public PublishedHarmonizedDatasetVariableResource getVariable(@PathParam("id") String id,
    @PathParam("study") String studyId, @PathParam("population") String populationId, @PathParam("dce") String dceId,
    @PathParam("variable") String variable) {
    checkAccess(id);
    PublishedHarmonizedDatasetVariableResource resource = applicationContext
      .getBean(PublishedHarmonizedDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    resource.setStudyId(studyId);
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, id);
    dataset.getStudyTables().stream().filter(t -> t.appliesTo(studyId, populationId, dceId)).forEach(t -> {
      resource.setProject(t.getProject());
      resource.setTable(t.getTable());
    });
    return resource;
  }

  private void checkAccess(String id) {
    subjectAclService.checkAccess("/harmonization-dataset", id);
  }
}
