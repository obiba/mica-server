/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.search.rest.harmonization.PublishedDataschemaDatasetVariableResource;
import org.obiba.mica.dataset.search.rest.harmonization.PublishedHarmonizedDatasetVariableResource;
import org.obiba.mica.dataset.search.rest.collection.PublishedStudyDatasetVariableResource;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.CollectionDatasetService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/")
@RequiresAuthentication
public class PublishedDatasetVariableResource {

  @Inject
  private CollectionDatasetService collectionDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private ApplicationContext applicationContext;

  @Path("/variable/{id}")
  public DatasetVariableResource getVariable(@PathParam("id") String id,
    @QueryParam("locale") @DefaultValue("en") String locale) {

    DatasetVariableResource resource = null;
    DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(id);
    switch(resolver.getType()) {
      case Collection:
        if (!collectionDatasetService.isPublished(resolver.getDatasetId())) throw NoSuchDatasetException.withId(resolver.getDatasetId());
        resource = applicationContext.getBean(PublishedStudyDatasetVariableResource.class);
        ((PublishedStudyDatasetVariableResource)resource).setLocale(locale);
        break;
      case Dataschema:
        if (!harmonizationDatasetService.isPublished(resolver.getDatasetId())) throw NoSuchDatasetException.withId(resolver.getDatasetId());
        resource = applicationContext.getBean(PublishedDataschemaDatasetVariableResource.class);
        ((PublishedDataschemaDatasetVariableResource)resource).setLocale(locale);
        break;
      case Harmonized:
        if (!harmonizationDatasetService.isPublished(resolver.getDatasetId())) throw NoSuchDatasetException.withId(resolver.getDatasetId());
        resource = applicationContext.getBean(PublishedHarmonizedDatasetVariableResource.class);
        ((PublishedHarmonizedDatasetVariableResource)resource).setStudyId(resolver.getStudyId());
        ((PublishedHarmonizedDatasetVariableResource)resource).setProject(resolver.getProject());
        ((PublishedHarmonizedDatasetVariableResource)resource).setTable(resolver.getTable());
        ((PublishedHarmonizedDatasetVariableResource)resource).setLocale(locale);
        break;
    }

    if (resource != null) {
      resource.setDatasetId(resolver.getDatasetId());
      resource.setVariableName(resolver.getName());
      return resource;
    }

    throw new IllegalArgumentException("Not a valid variable identifier: " + id);
  }

}
