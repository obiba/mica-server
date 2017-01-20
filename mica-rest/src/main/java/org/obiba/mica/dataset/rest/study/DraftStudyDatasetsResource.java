/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.study;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.DraftStudyDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

@Component
@Scope("request")
@Path("/draft")
public class DraftStudyDatasetsResource {

  private static final int MAX_LIMIT = 10000; //default ElasticSearch limit

  @Inject
  private StudyDatasetService datasetService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Helper helper;

  @Inject
  private DraftStudyDatasetService draftStudyDatasetService;

  /**
   * Get all {@link org.obiba.mica.dataset.domain.StudyDataset}, optionally filtered by study.
   *
   * @param studyId can be null, in which case all datasets are returned
   * @return
   */
  @GET
  @Path("/study-datasets")
  @Timed
  public List<Mica.DatasetDto> list(@QueryParam("study") String studyId, @QueryParam("query") String query,
                                    @QueryParam("from") @DefaultValue("0") Integer from,
                                    @QueryParam("limit") Integer limit, @Context HttpServletResponse response) {
    Stream<Mica.DatasetDto> result;
    long totalCount;

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    if(Strings.isNullOrEmpty(query)) {
      List<StudyDataset> datasets = datasetService.findAllDatasets(studyId).stream()
        .filter(s -> subjectAclService.isPermitted("/draft/study-dataset", "VIEW", s.getId())).collect(toList());
      totalCount = datasets.size();
      result = datasets.stream().map(d -> dtos.asDto(d, true)).skip(from).limit(limit);
    } else {
      DocumentService.Documents<StudyDataset> datasets = draftStudyDatasetService.find(from, limit, null, null, studyId, query);
      totalCount = datasets.getTotal();
      result = datasetService.findAllDatasets(datasets.getList().stream().map(Dataset::getId).collect(toList())).stream().map(d -> dtos.asDto(d, true));
    }

    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return result.collect(toList());
  }

  @POST
  @Path("/study-datasets")
  @Timed
  @RequiresPermissions({ "/draft/study-dataset:ADD" })
  public Response create(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo,
                         @Nullable @QueryParam("comment") String comment) {
    Dataset dataset = dtos.fromDto(datasetDto);
    if(!(dataset instanceof StudyDataset)) throw new IllegalArgumentException("An study dataset is expected");

    datasetService.save((StudyDataset) dataset, comment);
    return Response.created(uriInfo.getBaseUriBuilder().segment("draft", "study-dataset", dataset.getId()).build())
      .build();
  }

  @PUT
  @Path("/study-datasets/_index")
  @Timed
  @RequiresPermissions({ "/draft/study-dataset:PUBLISH" })
  public Response reIndex() {
    helper.indexAll();
    return Response.noContent().build();
  }

  @Path("/study-dataset/{id}")
  public DraftStudyDatasetResource dataset(@PathParam("id") String id) {
    DraftStudyDatasetResource resource = applicationContext.getBean(DraftStudyDatasetResource.class);
    resource.setId(id);
    return resource;
  }

  @Component
  public static class Helper {

    @Inject
    private StudyDatasetService studyDatasetService;

    @Async
    public void indexAll() {
      studyDatasetService.indexAll();
    }
  }

}
