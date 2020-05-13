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

(function () {

  const DEFAULT_LIMIT = 20;

  class PersonsListController {
    constructor($timeout, $location, ContactsSearchResource, EntityMembershipService) {
      this.$timeout = $timeout;
      this.$location = $location;
      this.ContactsSearchResource = ContactsSearchResource;
      this.EntityMembershipService = EntityMembershipService;
      this.loading = false;
      this.limit = DEFAULT_LIMIT;
      this.persons = [];
      this.total = 0;
      this._query = null;
      this.timeoutHandler = null;
      this.ngObibaStringUtils = new obiba.utils.NgObibaStringUtils();
    }

    __cleanupQuery(text) {
      const cleaners = [
        this.ngObibaStringUtils.cleanOrEscapeSpecialLuceneBrackets,
        this.ngObibaStringUtils.cleanDoubleQuotesLeftUnclosed,
        (text) => text.replace(/[!^~\\/]/g,''),
        (text) => text.match(/\*$/) === null ? `${text}*` : text,
      ];

      let cleaned = text;
      cleaners.forEach(cleaner => cleaned = cleaner.apply(null, [cleaned.trim()]));

      return cleaned && cleaned.length > 1 ? cleaned : null;
    }

    get query() {
      return this._query || null;
    }

    set query(text) {
      this._query = text ? text.trim() : null;

      if (this._query === 1) {
        return;
      }

      if (this.timeoutHandler) {
        this.$timeout.cancel(this.timeoutHandler);
      }

      this.timeoutHandler = this.$timeout((this.getPersons(this._query, 0)), 250);
    }

    getPersons(query, from, limit, exclude) {
      const searchQuery = query ? this.__cleanupQuery(query) : query;
      this.loading = true;
      this.ContactsSearchResource.search({
        query: searchQuery,
        from: from,
        limit: limit || DEFAULT_LIMIT,
        exclude: exclude
      }).$promise
        .then(result => {
          this.loading = false;
          this.persons = (result.persons || []).map((person) => {
            if (person.networkMemberships) {
              person.networks = this.EntityMembershipService.groupRolesByEntity('networks', person.networkMemberships);
            }

            if (person.studyMemberships) {
              person.studies = this.EntityMembershipService.groupRolesByEntity('studies', person.studyMemberships);
            }

            return person;
          });
          this.total = result.total;
        })
        .catch(error => {
          console.error(`Search failed for ${searchQuery} - ${error.data ? error.data.message : error.statusText}`);
          this.loading = false;
          this.persons = [];
          this.total = 0;
        });
    }

    $onInit() {
      const search = this.$location.search();
      if (search && 'exclude' in search) {
        this.getPersons(this._query, 0, this.limit, search.exclude);
        this.$location.search({}).replace();
      } else {
        this.getPersons(null, 0);
      }
    }

    onPageChanged(newPage/*, oldPage*/) {
      const from = DEFAULT_LIMIT * (newPage - 1);
      this.getPersons(this.query, from);
    }
  }

  mica.persons
    .component('personsList', {
      bindings: {
      },
      templateUrl: 'app/persons/views/persons-list.html',
      controllerAs: '$ctrl',
      controller: [
        '$timeout',
        '$location',
        'ContactsSearchResource',
        'EntityMembershipService',
        PersonsListController
      ]
    });

})();
