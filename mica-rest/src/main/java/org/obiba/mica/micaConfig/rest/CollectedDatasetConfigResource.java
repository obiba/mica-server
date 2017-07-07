/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import org.obiba.mica.micaConfig.domain.CollectedDatasetConfig;
import org.obiba.mica.micaConfig.service.StudyDatasetConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Component
@Path("/config/" + CollectedDatasetConfigResource.TARGET_NAME)
public class CollectedDatasetConfigResource
  extends EntityConfigResource<CollectedDatasetConfig, Mica.EntityFormDto>
  implements PermissionAwareResource {

  static final String TARGET_NAME = "collected-dataset";

  @Inject
  StudyDatasetConfigService datasetConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(CollectedDatasetConfig datasetConfig) {
    return dtos.asDto(datasetConfig);
  }

  @Override
  protected CollectedDatasetConfig fromDto(Mica.EntityFormDto dto) {
    return dtos.fromDto(dto);
  }

  @Override
  protected StudyDatasetConfigService getConfigService() {
    return datasetConfigService;
  }

  @Override
  public String getTarget() {
    return TARGET_NAME;
  }
}
