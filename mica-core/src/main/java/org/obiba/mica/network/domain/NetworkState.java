/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.domain;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.NetworkEntityState;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class NetworkState extends NetworkEntityState {

  private LocalizedString name;

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }
}
