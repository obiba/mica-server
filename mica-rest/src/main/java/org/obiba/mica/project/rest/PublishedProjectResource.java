/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.NoSuchMicaProjectException;
import org.obiba.mica.project.service.PublishedProjectService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Network.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedProjectResource {

  @Inject
  private PublishedProjectService publishedProjectService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.ProjectDto get() {
    checkAccess();
    return dtos.asDto(getProject());
  }

  private void checkAccess() {
    subjectAclService.checkAccess("/project", id);
  }

  private Project getProject() {
    Project project = publishedProjectService.findById(id);
    if (project == null) throw NoSuchMicaProjectException.withId(id);
    return project;
  }
}
