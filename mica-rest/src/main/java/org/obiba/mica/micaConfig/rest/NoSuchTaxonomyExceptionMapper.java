/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.obiba.opal.core.cfg.NoSuchTaxonomyException;

@Provider
public class NoSuchTaxonomyExceptionMapper implements ExceptionMapper<NoSuchTaxonomyException> {

  @Override
  public Response toResponse(NoSuchTaxonomyException exception) {
    return Response.status(Status.NOT_FOUND).build();
  }

}
