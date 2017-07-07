/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.dataset
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/collected-dataset', {
          templateUrl: 'app/dataset/views/collected-dataset-list.html',
          controller: 'CollectedDatasetListController'
        })
        .when('/collected-dataset/new', {
          templateUrl: 'app/dataset/views/collected-dataset-form.html',
          controller: 'CollectedDatasetEditController'
        })
        .when('/collected-dataset/:id/edit', {
          templateUrl: 'app/dataset/views/collected-dataset-form.html',
          controller: 'CollectedDatasetEditController'
        })
        .when('/harmonization-dataset', {
          templateUrl: 'app/dataset/views/harmonization-dataset-list.html',
          controller: 'HarmonizationDatasetListController'
        })
        .when('/harmonization-dataset/new', {
          templateUrl: 'app/dataset/views/harmonization-dataset-form.html',
          controller: 'HarmonizationDatasetEditController'
        })
        .when('/harmonization-dataset/:id/edit', {
          templateUrl: 'app/dataset/views/harmonization-dataset-form.html',
          controller: 'HarmonizationDatasetEditController'
        });

      ['collected-dataset', 'harmonization-dataset'].forEach(
        function (type) {
          $routeProvider
            .when('/' + type + '/:id', {
              templateUrl: 'app/dataset/views/dataset-view.html',
              controller: 'DatasetViewController'
            })
            .when('/' + type + '/:id/revisions', {
              templateUrl: 'app/dataset/views/dataset-view-revisions.html',
              controller: 'DatasetViewController'
            })
            .when('/' + type + '/:id/files', {
              templateUrl: 'app/dataset/views/dataset-view-files.html',
              controller: 'DatasetViewController',
              reloadOnSearch: false
            })
            .when('/' + type + '/:id/permissions', {
              templateUrl: 'app/dataset/views/dataset-view-permissions.html',
              controller: 'DatasetViewController'
            })
            .when('/' + type + '/:id/comments', {
              templateUrl: 'app/dataset/views/dataset-view-comments.html',
              controller: 'DatasetViewController'
            });
        }
      );

    }]);
