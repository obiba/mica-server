/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.io.Files;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.file.support.FileMediaType;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Component
@Path("/")
@RequiresAuthentication
public class PublishedFileSystemResource extends AbstractFileSystemResource {

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private TempFileService tempFileService;

  @Override
  protected boolean isPublishedFileSystem() {
    return true;
  }

  @GET
  @Path("/file-dl/{path:.*}")
  @Timed
  public Response downloadFile(@PathParam("path") String path,
    @QueryParam("inline") @DefaultValue("false") boolean inline) {

    try {
      Attachment attachment = doGetAttachment(path);

      if (inline) {
        String filename = attachment.getName();
        return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
          .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
          .type(FileMediaType.type(Files.getFileExtension(filename)))
          .build();
      }

      return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
        .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
    } catch (NoSuchEntityException e) {
      String name = doZip(path);

      return Response.ok(tempFileService.getInputStreamFromFile(name))
        .header("Content-Disposition", "attachment; filename=\"" + name + "\"").build();
    }
  }

  @GET
  @Path("/file/{path:.*}")
  @Timed
  public Mica.FileDto getFile(@PathParam("path") String path) {
    return doGetFile(path);
  }
}
