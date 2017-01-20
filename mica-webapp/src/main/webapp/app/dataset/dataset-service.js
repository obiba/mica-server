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
  .factory('StudyDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-datasets', {}, {
        'query': {method: 'GET', errorHandler: true, isArray: true},
        'delete': {method: 'DELETE', url: 'ws/draft/study-dataset/:id', params: {id: '@id'}, errorHandler: true}
      });
    }])

  .factory('DraftStudyDatasetsResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource('ws/draft/study-datasets?comment:comment', {}, {
        'save': {method: 'POST', errorHandler: true, transformRequest: function(dataset) {
          return DatasetModelService.serialize(dataset);
        }}
      });
    }])

  .factory('StudyDatasetResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource('ws/draft/study-dataset/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: function(dataset) {
          return DatasetModelService.serialize(dataset);
        }},
        'get': {method: 'GET', transformResponse: function(data) {
          return DatasetModelService.deserialize(data);
        }}
      });
    }])

  .factory('StudyDatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-dataset/:id/_publish', {}, {
        'publish': {method: 'PUT', params: {id: '@id'}},
        'unPublish': {method: 'DELETE', params: {id: '@id'}}
      });
    }])

  .factory('StudyStateProjectsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/study-state/:id/projects', {}, {
        'get': {method: 'GET', params: {id: '@id'}}
      });
    }])

  .factory('HarmonizationDatasetsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonization-datasets?comment:comment', {}, {
        'query': {method: 'GET', errorHandler: true, isArray: true},
        'delete': {method: 'DELETE', url: 'ws/draft/harmonization-dataset/:id', params: {id: '@id'}, errorHandler: true}
      });
    }])

  .factory('DraftHarmonizationDatasetsResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource('ws/draft/harmonization-datasets', {}, {
        'save': {method: 'POST', errorHandler: true, transformRequest: function(dataset) {
          return DatasetModelService.serialize(dataset);
        }}
      });
    }])

  .factory('HarmonizationDatasetResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource('ws/draft/harmonization-dataset/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: function(dataset) {
          return DatasetModelService.serialize(dataset);
        }},
        'get': {method: 'GET', transformResponse: function(data) {
          return DatasetModelService.deserialize(data);
        }}
      });
    }])

  .factory('HarmonizationDatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/harmonization-dataset/:id/_publish', {}, {
        'publish': {method: 'PUT', params: {id: '@id'}},
        'unPublish': {method: 'DELETE', params: {id: '@id'}}
      });
    }])

  .factory('DatasetResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource('ws/draft/:type/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id', type: '@type'}, errorHandler: true, transformRequest: function(dataset) {
          return DatasetModelService.serialize(dataset);
        }},
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type'}, errorHandler: true},
        'get': {method: 'GET', params: {id: '@id', type: '@type'}, transformResponse: function(data) {
          return DatasetModelService.deserialize(data);
        }}
      });
    }])

  .factory('DatasetPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/_publish', {id: '@id', type: '@type'}, {
        'publish': {method: 'PUT', params: {cascading: '@cascading'}},
        'unPublish': {method: 'DELETE'}
      });
    }])

  .factory('DraftDatasetStatusResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/_status', {}, {
        'toStatus': {method: 'PUT', params: {id: '@id', type: '@type', value: '@value'}}
      });
    }])

  .factory('DraftDatasetRevisionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/commits', {}, {
        'get': {method: 'GET'}
      });
    }])

  .factory('DraftDatasetPermissionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:datasetType/:id/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', role: '@role'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftDatasetAccessesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:datasetType/:id/accesses', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftDatasetRestoreRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/:type/:id/commit/:commitId/restore', {}, {
        'restore': {method: 'PUT', params: {type: '@type', id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('DraftDatasetViewRevisionResource', ['$resource', 'DatasetModelService',
    function ($resource, DatasetModelService) {
      return $resource('ws/draft/:type/:id/commit/:commitId/view', {}, {
        'view': {method: 'GET', transformResponse: function(data) {
          return DatasetModelService.deserialize(data);
        }}
      });
    }])

  .factory('DatasetService', ['$rootScope',
    'HarmonizationDatasetResource',
    'NOTIFICATION_EVENTS',
    function ($rootScope, HarmonizationDatasetResource, NOTIFICATION_EVENTS) {

      function getNames(name) {
        return name.map(function(entry) {
          return entry.value;
        }).join('-');
      }

      return {
        deleteDataset: function (dataset, onSuccess) {
          var datasetToDelete = dataset;

          var removeSubscriber = $rootScope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
            if (datasetToDelete.id === id) {
              dataset.$delete(onSuccess);
            }
            removeSubscriber();
          });

          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'dataset.delete-dialog.title',
              messageKey: 'dataset.delete-dialog.message',
              messageArgs: [getNames(dataset.name)]
            }, dataset.id
          );
        }
      };
    }])

  .factory('OpalTablesService', [function() {
    var factory = {};
    var tableWrappers = null;

    function findTargetTables(dataset, type) {
      switch (type) {
        case mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE:
          return dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables;
        case mica.dataset.OPAL_TABLE_TYPES.NETWORK_TABLE:
          return dataset['obiba.mica.HarmonizationDatasetDto.type'].networkTables;
      }

      throw new Error('Invalid table type');
    }

    function createTargetTables(dataset, type) {
      var tablesName;
      switch (type) {
        case mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE:
          tablesName = 'studyTables';
          break;
        case mica.dataset.OPAL_TABLE_TYPES.NETWORK_TABLE:
          tablesName = 'networkTables';
          break;
        default:
          throw new Error('Invalid table type');
      }

      dataset['obiba.mica.HarmonizationDatasetDto.type'][tablesName] = dataset['obiba.mica.HarmonizationDatasetDto.type'][tablesName] || [];
      return dataset['obiba.mica.HarmonizationDatasetDto.type'][tablesName];
    }

    factory.updateTable = function(dataset, wrapper, newTable) {
      var tables = findTargetTables(dataset, wrapper.type);

      var index = tables.indexOf(wrapper.table);
      if (index === -1) {
        throw new Error('Wrapper table is not found.');
      }

      tables[index] = newTable;
      wrapper.table = newTable;
    };

    factory.addTable = function(dataset, type, newTable) {
      var tables = createTargetTables(dataset, type);
      tables.push(newTable);
      tableWrappers = tableWrappers || [];
      tableWrappers.push({type: type, table: newTable});
    };

    factory.addUpdateTable = function(dataset, tableType, wrapper, newTable) {
      if (angular.isDefined(wrapper)) {
        this.updateTable(dataset, wrapper, newTable);
      } else {
        this.addTable(dataset, tableType, newTable);
      }

      return tableWrappers;
    };

    factory.setTable = function(dataset, newTable) {
      if (!dataset['obiba.mica.StudyDatasetDto.type']) {
        dataset['obiba.mica.StudyDatasetDto.type'] = {};
      }
      dataset['obiba.mica.StudyDatasetDto.type'].studyTable = newTable;
    };

    factory.getTables = function getOpalTables(dataset) {
      tableWrappers = [];

      if (dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables) {
        tableWrappers = dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables.map(function (studyTable) {
          return {type: mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE, table: studyTable};
        });
      }

      if (dataset['obiba.mica.HarmonizationDatasetDto.type'].networkTables) {
        tableWrappers = tableWrappers.concat(
          dataset['obiba.mica.HarmonizationDatasetDto.type'].networkTables.map(function (networkTable) {
            return {type: mica.dataset.OPAL_TABLE_TYPES.NETWORK_TABLE, table: networkTable};
          })
        );
      }

      tableWrappers = tableWrappers.sort(function(a,b){
        return a.table.weight - b.table.weight;
      });

      return tableWrappers;
    };

    factory.deleteTable = function(dataset, wrapper) {
      var wrapperIndex = tableWrappers.indexOf(wrapper);
      var tables = findTargetTables(dataset, wrapper.type);
      var index = tables.indexOf(wrapper.table);

      if (index > -1) {
        tables.splice(index, 1);
        if (tables.length === 0) {
          var tablesName = wrapper.type === mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE ? 'studyTables' : 'networkTables';
          dataset['obiba.mica.HarmonizationDatasetDto.type'][tablesName] = undefined;
        }

        tableWrappers.splice(wrapperIndex, 1);
      }
    };

    factory.deleteTables = function (dataset, wrappers) {
      if (Array.isArray(wrappers)) {
        wrappers.forEach(function (w) {
          factory.deleteTable(dataset, w);
        });
      }
    };

    factory.updateWeights = function() {
      if (tableWrappers) {
        for (var i = 0; i < tableWrappers.length;  i++) {
          tableWrappers[i].table.weight = i;
        }
      }
    };

    return factory;
  }])

  .service('DatasetModelService',['LocalizedValues', function(LocalizedValues) {
    this.serialize = function(dataset) {
      var datasetCopy = angular.copy(dataset);
      datasetCopy.name = LocalizedValues.objectToArray(datasetCopy.model._name);
      datasetCopy.acronym = LocalizedValues.objectToArray(datasetCopy.model._acronym);
      datasetCopy.description = LocalizedValues.objectToArray(datasetCopy.model._description);
      datasetCopy.entityType = datasetCopy.model._entityType;
      delete datasetCopy.model._name;
      delete datasetCopy.model._acronym;
      delete datasetCopy.model._description;
      delete datasetCopy.model._entityType;
      datasetCopy.content = datasetCopy.model ? angular.toJson(datasetCopy.model) : null;
      delete datasetCopy.model; // NOTICE: must be removed to avoid protobuf exception in dto.
      return angular.toJson(datasetCopy);
    };

    this.deserialize = function(data) {
      var dataset = angular.fromJson(data);
      dataset.model = dataset.content ? angular.fromJson(dataset.content) : {};
      dataset.model._name = LocalizedValues.arrayToObject(dataset.name);
      dataset.model._acronym = LocalizedValues.arrayToObject(dataset.acronym);
      dataset.model._description = LocalizedValues.arrayToObject(dataset.description);
      dataset.model._entityType = dataset.entityType;
      return dataset;
    };

    return this;
  }]);
