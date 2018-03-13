/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.variable.search.rest;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.ComposedSet;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.SetOperation;
import org.obiba.mica.dataset.service.VariableSetOperationService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/variables/sets")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetVariablesSetsResource {

  @Inject
  private VariableSetService variableSetService;

  @Inject
  private VariableSetOperationService variableSetOperationService;

  @Inject
  private Dtos dtos;

  @GET
  public List<Mica.DocumentSetDto> list(@QueryParam("id") List<String> ids) {
    if (ids.isEmpty())
      return variableSetService.getAllCurrentUser().stream().map(s -> dtos.asDto(s)).collect(Collectors.toList());
    else
      return ids.stream().map(id -> dtos.asDto(variableSetService.get(id))).collect(Collectors.toList());
  }

  @POST
  public Response createEmpty(@Context UriInfo uriInfo, @QueryParam("name") String name) {
    DocumentSet created = variableSetService.create(name, Lists.newArrayList());
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "set", created.getId()).build()).build();
  }

  @POST
  @Path("_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importVariables(@Context UriInfo uriInfo, @QueryParam("name") String name, String body) {
    DocumentSet created = variableSetService.create(name, variableSetService.extractIdentifiers(body));
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "set", created.getId()).build())
      .entity(dtos.asDto(created)).build();
  }

  @POST
  @Path("operations")
  public Response compose(@Context UriInfo uriInfo, @QueryParam("s1") String set1, @QueryParam("s2") String set2, @QueryParam("s3") String set3) {
    List<DocumentSet> sets = Lists.newArrayList();
    sets.add(variableSetService.get(set1));
    sets.add(variableSetService.get(set2));
    if (!Strings.isNullOrEmpty(set3)) sets.add(variableSetService.get(set3));
    SetOperation setOperation = variableSetOperationService.create(sets);
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "sets", "operation", setOperation.getId()).build()).build();
  }

  @GET
  @Path("operation/{id}")
  public Mica.SetOperationDto compose(@Context UriInfo uriInfo, @PathParam("id") String operationId) {
    return dtos.asDto(variableSetOperationService.get(operationId));
  }
}
