/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonization;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetsResource;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@Path("/harmonization-datasets")
@RequiresAuthentication
public class PublishedHarmonizationDatasetsResource extends AbstractPublishedDatasetsResource<HarmonizationDataset> {

  private static final Logger log = LoggerFactory.getLogger(PublishedHarmonizationDatasetsResource.class);

  /**
   * Get {@link org.obiba.mica.dataset.domain.HarmonizationDataset}s, optionally filtered by study.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @param studyId
   * @return
   */
  @GET
  @Timed
  public Mica.DatasetsDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
      @QueryParam("order") String order, @QueryParam("study") String studyId, @QueryParam("query") String query) {

    return getDatasetDtos(HarmonizationDataset.class, from, limit, sort, order, studyId, query);
  }

  @Override
  protected String getStudyIdField() {
    return "studyTable.studyId";
  }
}
