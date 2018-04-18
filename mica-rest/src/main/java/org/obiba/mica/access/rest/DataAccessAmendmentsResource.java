package org.obiba.mica.access.rest;


import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.DataAccessRequestStatus;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
public class DataAccessAmendmentsResource {

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessAmendmentService dataAccessAmendmentService;

  private String parentId;

  @POST
  @Timed
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    String resource = String.format("/data-access-request/%s/amendment", parentId);
    subjectAclService.checkPermission(resource, "ADD");

    DataAccessAmendment amendment = dtos.fromAmendmentDto(dto);

    // force applicant and make sure it is a new request
    String applicant = SecurityUtils.getSubject().getPrincipal().toString();
    amendment.setApplicant(applicant);
    amendment.setId(null);
    amendment.setParentId(parentId);
    amendment.setStatus(DataAccessRequestStatus.OPENED);

    dataAccessAmendmentService.save(amendment);
    resource = String.format("/data-access-request/%s/amendment", parentId);

    subjectAclService.addPermission(resource, "VIEW,EDIT,DELETE", amendment.getId());
    subjectAclService.addPermission(resource + "/" + amendment.getId(), "EDIT", "_status");

    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", parentId, "amendment", amendment.getId()).build()).build();
  }

  @GET
  @Timed
  public List<Mica.DataAccessRequestDto> listByStatus(@QueryParam("status") List<String> status) {
    return listByStatusFilteringPermitted(status).stream()
      .map(dtos::asAmendentDto)
      .collect(Collectors.toList());
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  private List<DataAccessAmendment> listByStatusFilteringPermitted(List<String> status) {
    String resource = String.format("/data-access-request/%s/amendment", parentId);
    List<DataAccessAmendment> amendments = dataAccessAmendmentService.findByStatus(parentId, status);
    return amendments.stream() //
      .filter(amendment -> subjectAclService.isPermitted(resource, "VIEW", amendment.getId())) //
      .collect(Collectors.toList());
  }
}
