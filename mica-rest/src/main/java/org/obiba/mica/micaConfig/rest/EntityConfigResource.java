/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.service.EntityConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

@Component
public abstract class EntityConfigResource<T extends EntityConfig> {

  @Inject
  ApplicationContext applicationContext;

  @Inject
  Dtos dtos;

  @Inject
  EntityConfigTranslator entityConfigTranslator;

  protected abstract Mica.EntityFormDto asDto(T entityConfig);

  protected abstract T fromDto(Mica.EntityFormDto entityConfig);

  @GET
  @Path("/form")
  public Mica.EntityFormDto get(@Context UriInfo uriInfo, @QueryParam("locale") String locale) {
    Optional<T> d = getConfigService().find();
    if(!d.isPresent()) throw NoSuchEntityException.withPath(EntityConfig.class, uriInfo.getPath());

    T config = d.get();
    entityConfigTranslator.translateSchema(locale, config);

    return asDto(config);
  }

  @PUT
  @Path("/form")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.EntityFormDto dto) {
    getConfigService().createOrUpdate(fromDto(dto));
    return Response.ok().build();
  }

  protected abstract EntityConfigService<T> getConfigService();

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }
}
