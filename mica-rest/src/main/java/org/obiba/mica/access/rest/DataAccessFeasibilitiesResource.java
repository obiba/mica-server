package org.obiba.mica.access.rest;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.service.DataAccessFeasibilityService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessFeasibilitiesResource {

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private DataAccessFeasibilityService dataAccessFeasibilityService;

  private String parentId;

  @POST
  @Timed
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    DataAccessFeasibility feasibility = dtos.fromFeasibilityDto(dto);
    return saveNew(feasibility, uriInfo);
  }

  @POST
  @Path("/_empty")
  public Response create(@Context UriInfo uriInfo) {
    DataAccessFeasibility feasibility = new DataAccessFeasibility();
    feasibility.setContent("{}");
    return saveNew(feasibility, uriInfo);
  }

  private Response saveNew(DataAccessFeasibility feasibility, UriInfo uriInfo) {
    String resource = String.format("/data-access-request/%s/feasibilities", parentId);
    subjectAclService.checkPermission(resource, "ADD");

    // force applicant and make sure it is a new request
    String applicant = SecurityUtils.getSubject().getPrincipal().toString();
    feasibility.setApplicant(applicant);
    feasibility.setId(null);
    feasibility.setParentId(parentId);
    feasibility.setStatus(DataAccessEntityStatus.OPENED);

    dataAccessFeasibilityService.save(feasibility);
    resource = String.format("/data-access-request/%s/feasibility", parentId);

    subjectAclService.addPermission(resource, "VIEW,EDIT,DELETE", feasibility.getId());
    subjectAclService.addPermission(resource + "/" + feasibility.getId(), "EDIT", "_status");

    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", parentId, "feasibility", feasibility.getId()).build()).build();
  }

  @GET
  @Timed
  public List<Mica.DataAccessRequestDto> listByStatus(@QueryParam("status") List<String> status) {
    return listByStatusFilteringPermitted(status).stream()
      .map(dtos::asFeasibilityDto)
      .collect(Collectors.toList());
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @GET
  @Path("/_history")
  public List<StatusChangeDto> getLoggedHistory() {
    List<StatusChangeDto> statusChangeDtos = new ArrayList<>();
    dataAccessFeasibilityService.findByParentId(parentId).forEach(feasibility ->
      statusChangeDtos.addAll(dtos.asStatusChangeDtoList(feasibility))
    );

    return statusChangeDtos;
  }

  private List<DataAccessFeasibility> listByStatusFilteringPermitted(List<String> status) {
    String resource = "/data-access-request";
    List<DataAccessFeasibility> feasibilities = dataAccessFeasibilityService.findByStatus(parentId, status);
    return feasibilities.stream() //
      .filter(feasibility -> subjectAclService.isPermitted(resource, "VIEW", parentId)) //
      .collect(Collectors.toList());
  }
}
