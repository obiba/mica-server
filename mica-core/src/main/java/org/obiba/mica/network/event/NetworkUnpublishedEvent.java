/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.study.domain.Study;

public class NetworkUnpublishedEvent extends PersistablePublishedEvent<Network> {

  public NetworkUnpublishedEvent(Network network) {
    super(network);
  }
}
