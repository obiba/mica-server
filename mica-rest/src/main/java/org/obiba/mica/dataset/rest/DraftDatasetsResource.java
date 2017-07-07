/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
@Scope("request")
@Path("/draft/datasets")
public class DraftDatasetsResource {

  @Inject
  private EventBus eventBus;

  @PUT
  @Path("/_index")
  @RequiresPermissions({ "/draft/collected-dataset:EDIT", "/draft/harmonization-dataset:EDIT" })
  public Response indexAll() {
    eventBus.post(new IndexDatasetsEvent());
    return Response.noContent().build();
  }
}
