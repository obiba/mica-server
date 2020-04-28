/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.persons = angular.module('mica.persons', [
  'obiba.form',
  'mica.config',
  'obiba.notification',
  'obiba.mica.localized',
  'pascalprecht.translate',
  'ui.bootstrap'
]);

mica.persons.service('EntityTitleService', ['$filter', function($filter) {
    function translate(entityType, plural) {
      return plural
        ? $filter('translate')(entityType === 'network' ? 'networks' : 'studies')
        : $filter('translate')(`${entityType}.label`);
    }

    this.translate = translate;

    return this;
  }]);
