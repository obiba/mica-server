/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/")
@RequiresAuthentication
public class PublishedNetworksResource {

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Path("/networks")
  @Timed
  public List<Mica.NetworkDto> list() {
    return publishedNetworkService.findAll().stream().map(dtos::asDto).collect(Collectors.toList());
  }

  @Path("/network/{id}")
  public PublishedNetworkResource network(@PathParam("id") String id) {
    PublishedNetworkResource networkResource = applicationContext.getBean(PublishedNetworkResource.class);
    networkResource.setId(id);
    return networkResource;
  }

}
