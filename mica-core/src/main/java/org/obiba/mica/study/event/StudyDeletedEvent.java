/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.event;

import org.obiba.mica.core.event.PersistableDeletedEvent;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;

public class StudyDeletedEvent extends PersistableDeletedEvent<BaseStudy> {

  public StudyDeletedEvent(BaseStudy study) {
    super(study);
  }
}
