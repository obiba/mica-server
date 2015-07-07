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

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Search;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * Study variable resource: variable describing a study dataset.
 */
@Component
@Scope("request")
@Path("/study-dataset/{id}")
@RequiresAuthentication
public class PublishedStudyDatasetResource extends AbstractPublishedDatasetResource<StudyDataset> {

  @PathParam("id")
  private String id;

  @Inject
  private StudyDatasetService datasetService;

  /**
   * Get {@link org.obiba.mica.dataset.domain.StudyDataset} from published index.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @return
   */
  @GET
  public Mica.DatasetDto get() {
    return getDatasetDto(StudyDataset.class, id);
  }


  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable}s from published index.
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
  public Mica.DatasetVariablesDto queryVariables(@QueryParam("query") String queryString,
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit,
    @QueryParam("sort") String sort, @QueryParam("order") String order) {

    return getDatasetVariableDtos(queryString, id, DatasetVariable.Type.Study, from, limit, sort, order);
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable}s from published index.
   *
   * @return
   */
  @GET
  @Path("/variables")
  public Mica.DatasetVariablesDto getVariables(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
    @QueryParam("order") String order) {
    return getDatasetVariableDtos(id, DatasetVariable.Type.Study, from, limit, sort, order);
  }

  @Path("/variable/{variable}")
  public PublishedStudyDatasetVariableResource getVariable(@PathParam("variable") String variable) {
    PublishedStudyDatasetVariableResource resource = applicationContext
      .getBean(PublishedStudyDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  @POST
  @Path("/facets")
  public Search.QueryResultDto getFacets(Search.QueryTermsDto query) {
    return datasetService.getFacets(getDataset(StudyDataset.class, id), query);
  }

  @GET
  @Path("/contingency")
  public Search.QueryResultDto getContingency(@QueryParam("variable") String variable,
    @QueryParam("by") String crossVariable) {
    if(Strings.isNullOrEmpty(variable) || Strings.isNullOrEmpty(crossVariable))
      throw new BadRequestException("Variable names are required for the contingency table");
    return datasetService.getContingencyTable(getDataset(StudyDataset.class, id), variable, crossVariable);
  }

}
