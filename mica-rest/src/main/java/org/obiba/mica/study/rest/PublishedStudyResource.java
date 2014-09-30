package org.obiba.mica.study.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Study.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedStudyResource {

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.StudyDto get() {
    Study study = publishedStudyService.findById(id);
    if(study == null) throw NoSuchStudyException.withId(id);
    return dtos.asDto(study);
  }

  @Path("/file/{fileId}")
  public FileResource study(@PathParam("fileId") String fileId) {
    FileResource studyResource = applicationContext.getBean(FileResource.class);
    studyResource.setPersistable(publishedStudyService.findById(id));
    studyResource.setFileId(fileId);
    return studyResource;
  }

}
