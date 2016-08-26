'use strict';

mica.dataset

  .controller('StudyDatasetListController', ['$rootScope', '$scope', '$timeout', 'StudyDatasetsResource',
    'DatasetService',
    function ($rootScope, $scope, $timeout, StudyDatasetsResource, DatasetService) {
      var onSuccess = function(response, responseHeaders) {
        $scope.totalCount = parseInt(responseHeaders('X-Total-Count'), 10);
        $scope.studyDatasets = response;
        $scope.loading = false;

        if (!$scope.hasDatasets) {
          $scope.hasDatasets = $scope.totalCount && !$scope.pagination.searchText;
        }
      };

      var onError = function() {
        $scope.loading = false;
      };

      $scope.pageChanged = function(page) {
        loadPage(page, $scope.pagination.searchText);
      };

      function refreshPage() {
        if($scope.pagination.current !== 1) {
          $scope.pagination.current = 1; //pageChanged event triggers reload
        } else {
          loadPage(1);
        }
      }

      function loadPage(page) {
        var data = {from:(page - 1) * $scope.limit, limit: $scope.limit};

        if($scope.pagination.searchText) {
          data.query = $scope.pagination.searchText + '*';
        }

        StudyDatasetsResource.query(data, onSuccess, onError);
      }

      $scope.loading = true;
      $scope.hasStudies = false;
      $scope.pagination = {current: 1, searchText: ''};
      $scope.totalCount = 0;
      $scope.limit = 20;

      $scope.deleteStudyDataset = function (dataset) {
        DatasetService.deleteDataset(dataset, function () {
          refreshPage();
        });
      };

      var currentSearch = null;

      $scope.$watch('pagination.searchText', function(newVal, oldVal) {
        if (!newVal && !oldVal) {
          return;
        }

        if(currentSearch) {
          $timeout.cancel(currentSearch);
        }

        currentSearch = $timeout(function() {
          refreshPage();
        }, 500);
      });

      loadPage($scope.pagination.current);
    }])

  .controller('StudyDatasetEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    '$filter',
    'StudyDatasetResource',
    'DraftStudyDatasetsResource',
    'StudyDatasetPublicationResource',
    'MicaConfigResource',
    'FormServerValidation',
    'StudyStatesResource',
    'StudyStateProjectsResource',
    'FormDirtyStateObserver',
    'EntityFormResource',
    'LocalizedSchemaFormService',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              $filter,
              StudyDatasetResource,
              DraftStudyDatasetsResource,
              StudyDatasetPublicationResource,
              MicaConfigResource,
              FormServerValidation,
              StudyStatesResource,
              StudyStateProjectsResource,
              FormDirtyStateObserver,
              EntityFormResource,
              LocalizedSchemaFormService) {
      $scope.studies = [];
      $scope.projects = [];
      $scope.selected = {};
      $scope.studyTable = {};

      var populateStudyTable = function (studyTable) {
        if (studyTable !== {}) {
          $scope.selected.study = {
            id: studyTable.studyId,
            population: {
              id: studyTable.populationId,
              dataCollectionEvent: {id: studyTable.dataCollectionEventId}
            }
          };

          $scope.selected.project = {name: studyTable.project, table: studyTable.table};
        }

        StudyStatesResource.query().$promise.then(function (studies) {
          $scope.studies = studies.sort(function (a, b) { return a.id.localeCompare(b.id); });

          var selectedPopulation, selectedDce, selectedStudy = $scope.studies.filter(function (s) {return s.id === studyTable.studyId; })[0];

          if (selectedStudy) {
            $scope.selected.study = selectedStudy;
            selectedPopulation = selectedStudy.populationSummaries.filter(function (p) { return p.id === studyTable.populationId; })[0];

            if (selectedPopulation) {
              $scope.selected.study.population = selectedPopulation;

              selectedDce = selectedPopulation.dataCollectionEventSummaries.filter(function (dce) { return dce.id === studyTable.dataCollectionEventId; })[0];

              if (selectedDce) {
                $scope.selected.study.population.dataCollectionEvent = selectedDce;
              }
            }
          }
        });
      };

      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      $scope.type = getTypeFromUrl();
      $scope.activeTab = 0;
      $scope.newDataset = !$routeParams.id;
      if ($routeParams.id) {
        $scope.dataset = StudyDatasetResource.get({id: $routeParams.id}, function (dataset) {
          $scope.studyTable = dataset['obiba.mica.StudyDatasetDto.type'].studyTable;
          populateStudyTable($scope.studyTable);
        });
      } else {
        $scope.dataset = {
          published: false,
          'obiba.mica.StudyDatasetDto.type': {studyTable: {}},
          model: {}
        };

        populateStudyTable($scope.studyTable);
      }

      $scope.$watch('selected.study', function () {
        if ($scope.selected.study && $scope.selected.study.id) {
          StudyStateProjectsResource.query({id: $scope.selected.study.id}).$promise.then(function (projects) {
            $scope.projects = projects;
            var selectedTable, selectedProject = $scope.projects.filter(function (p) {return p.name === $scope.studyTable.project; })[0];

            if (selectedProject) {
              $scope.selected.project = selectedProject;

              selectedTable = selectedProject.datasource.table.filter(function (t) {return t === $scope.studyTable.table; })[0];

              if (selectedTable) {
                $scope.selected.project.table = selectedTable;
              }
            }
          });
        }
      });

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        $scope.sfOptions = {};

        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
          $scope.sfOptions[lang] = {formDefaults: {languages: {}}};
          $scope.sfOptions[lang].formDefaults.languages[lang]= $filter('translate')('language.' + lang);
        });

        EntityFormResource.get({target: 'dataset'}, function(form) {
          form.schema = LocalizedSchemaFormService.translate(angular.fromJson(form.schema));
          form.definition = LocalizedSchemaFormService.translate(angular.fromJson(form.definition));
          $scope.sfForm = form;
        });

      });

      $scope.save = function () {
        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }

        angular.extend($scope.dataset['obiba.mica.StudyDatasetDto.type'].studyTable, {
          studyId: $scope.selected.study.id,
          populationId: $scope.selected.study.population.id,
          dataCollectionEventId: $scope.selected.study.population.dataCollectionEvent.id,
          project: $scope.selected.project.name,
          table: $scope.selected.project.table
        });

        if ($scope.dataset.id) {
          updateDataset();
        } else {
          createDataset();
        }
      };

      $scope.cancel = function () {
        $location.path('/study-dataset' + ($scope.dataset.id ? '/' + $scope.dataset.id : '')).replace();
      };

      var updateDataset = function () {
        $scope.dataset.$save(
          function (dataset) {
            FormDirtyStateObserver.unobserve();
            $location.path('/study-dataset/' + dataset.id).replace();
          },
          saveErrorHandler);
      };

      var createDataset = function () {
        DraftStudyDatasetsResource.save($scope.dataset,
          function (resource, getResponseHeaders) {
            FormDirtyStateObserver.unobserve();
            var parts = getResponseHeaders().location.split('/');
            $location.path('/study-dataset/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      FormDirtyStateObserver.observe($scope);
    }])

  .controller('HarmonizationDatasetEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    '$uibModal',
    '$filter',
    'HarmonizationDatasetResource',
    'DraftHarmonizationDatasetsResource',
    'HarmonizationDatasetPublicationResource',
    'MicaConfigResource',
    'FormServerValidation',
    'DraftNetworksResource',
    'MicaConfigOpalProjectsResource',
    'FormDirtyStateObserver',
    'EntityFormResource',
    'LocalizedSchemaFormService',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              $uibModal,
              $filter,
              HarmonizationDatasetResource,
              DraftHarmonizationDatasetsResource,
              HarmonizationDatasetPublicationResource,
              MicaConfigResource,
              FormServerValidation,
              DraftNetworksResource,
              MicaConfigOpalProjectsResource,
              FormDirtyStateObserver,
              EntityFormResource,
              LocalizedSchemaFormService) {

      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      $scope.selected = {network: {}};
      $scope.networks = DraftNetworksResource.query();
      $scope.type = getTypeFromUrl();
      $scope.activeTab = 0;
      $scope.newDataset = !$routeParams.id;

      function getOpalProjects() {
        return MicaConfigOpalProjectsResource.get().$promise.then(function(projects){
          $scope.projects = projects;
        });
      }

      if ($routeParams.id) {
        $scope.dataset = HarmonizationDatasetResource.get({id: $routeParams.id});
        $scope.dataset.$promise.then(function (dataset) {

          $scope.networks.$promise.then(function (networks) {
            $scope.selected.network = networks.filter(function (n) {return n.id === dataset['obiba.mica.HarmonizationDatasetDto.type'].networkId; })[0];
          });

          getOpalProjects().then(function() {
            $scope.selected.project = $scope.projects.filter(function (p) {
              return p.name === dataset['obiba.mica.HarmonizationDatasetDto.type'].project;
            }).pop();

            if ($scope.selected.project) {
              $scope.selected.project.table = $scope.selected.project.datasource.table.filter(function (t) {
                return t === dataset['obiba.mica.HarmonizationDatasetDto.type'].table;
              }).pop();
            }
          });
        });
      } else {
        getOpalProjects();
        $scope.dataset = {
          published: false,
          'obiba.mica.HarmonizationDatasetDto.type': {},
          model: {}
        };
      }

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        $scope.sfOptions = {};

        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
          $scope.sfOptions[lang] = {formDefaults: {languages: {}}};
          $scope.sfOptions[lang].formDefaults.languages[lang]= $filter('translate')('language.' + lang);
        });

        EntityFormResource.get({target: 'dataset'}, function(form) {
          form.schema = LocalizedSchemaFormService.translate(angular.fromJson(form.schema));
          form.definition = LocalizedSchemaFormService.translate(angular.fromJson(form.definition));
          $scope.sfForm = form;
        });

        MicaConfigOpalProjectsResource.get().$promise.then(function(projects){
          $scope.projects = projects;
        });
      });

      $scope.save = function () {
        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }

        $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].project = $scope.selected.project.name;
        $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].table = $scope.selected.project.table;
        $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].networkId = $scope.selected.network ? $scope.selected.network.id : null;

        if ($scope.dataset.id) {
          updateDataset();
        } else {
          createDataset();
        }
      };

      $scope.addStudyTable = function (tab) {
        $uibModal
          .open({
            templateUrl: 'app/dataset/views/study-table-modal-form.html',
            controller: 'StudyTableModalController',
            resolve: {
              studyTable: function () {
                return {};
              },
              tab : function () {
                return tab;
              }
            }
          })
          .result.then(function (studyTable) {
            if (!$scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables) {
              $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables = [];
            }
            $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables.push(studyTable);
          }, function () {
          });
      };

      $scope.moveStudyDown = function (index) {
        var studyToMoveDown = $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables[index];
        var studyToMoveUp = $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables[index + 1];

        $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables[index] = studyToMoveUp;
        $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables[index + 1] = studyToMoveDown;
      };

      $scope.moveStudyUp = function (index) {
        $scope.moveStudyDown(index - 1);
      };

      $scope.editStudyTable = function (index, tab) {
        $uibModal
          .open({
            templateUrl: 'app/dataset/views/study-table-modal-form.html',
            controller: 'StudyTableModalController',
            resolve: {
              tab: function () {
                return tab;
              },
              studyTable: function () {
                return $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables[index];
              }
            }
          })
          .result.then(function (studyTable) {
            $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables[index] = studyTable;
          }, function () {
          });
      };

      $scope.deleteStudyTable = function (index) {
        $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables.splice(index, 1);
        if ($scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables.length === 0) {
          $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables = undefined;
        }
      };

      $scope.cancel = function () {
        $location.path('/harmonization-dataset' + ($scope.dataset.id ? '/' + $scope.dataset.id : '')).replace();
      };

      var updateDataset = function () {
        $scope.dataset.$save(
          function (dataset) {
            FormDirtyStateObserver.unobserve();
            $location.path('/harmonization-dataset/' + dataset.id).replace();
          },
          saveErrorHandler);
      };

      var createDataset = function () {
        DraftHarmonizationDatasetsResource.save($scope.dataset,
          function (resource, getResponseHeaders) {
            FormDirtyStateObserver.unobserve();
            var parts = getResponseHeaders().location.split('/');
            $location.path('/harmonization-dataset/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      FormDirtyStateObserver.observe($scope);
    }])

  .controller('DatasetViewController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    'DatasetResource',
    'DatasetPublicationResource',
    'DraftDatasetStatusResource',
    'DraftDatasetViewRevisionResource',
    'DraftDatasetRevisionsResource',
    'DraftDatasetRestoreRevisionResource',
    'DraftFileSystemSearchResource',
    'MicaConfigResource',
    'NOTIFICATION_EVENTS',
    '$filter',
    'DatasetService',
    'DocumentPermissionsService',
    'DraftNetworkResource',
    'EntityFormResource',
    'LocalizedSchemaFormService',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              DatasetResource,
              DatasetPublicationResource,
              DraftDatasetStatusResource,
              DraftDatasetViewRevisionResource,
              DraftDatasetRevisionsResource,
              DraftDatasetRestoreRevisionResource,
              DraftFileSystemSearchResource,
              MicaConfigResource,
              NOTIFICATION_EVENTS,
              $filter,
              DatasetService,
              DocumentPermissionsService,
              DraftNetworkResource,
              EntityFormResource,
              LocalizedSchemaFormService) {
      MicaConfigResource.get(function (micaConfig) {
        $scope.opal = micaConfig.opal;
        $scope.tabs = [];
        $scope.sfOptions = {};

        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
          $scope.sfOptions[lang] = {formDefaults: {languages: {}, readonly: true}};
          $scope.sfOptions[lang].formDefaults.languages[lang]= $filter('translate')('language.' + lang);
        });

        $scope.openAccess = micaConfig.openAccess;

        EntityFormResource.get({target: 'dataset'}, function(form) {
          form.schema = LocalizedSchemaFormService.translate(angular.fromJson(form.schema));
          form.definition = LocalizedSchemaFormService.translate(angular.fromJson(form.definition));
          $scope.sfForm = form;
        });
      });

      var initializeDataset = function(dataset) {
        $scope.permissions = DocumentPermissionsService.state(dataset['obiba.mica.EntityStateDto.datasetState']);
        if($scope.type === 'harmonization-dataset') {
          if(dataset['obiba.mica.HarmonizationDatasetDto.type'].networkId) {
            DraftNetworkResource.get({id: dataset['obiba.mica.HarmonizationDatasetDto.type'].networkId}).$promise.then(function (network) {
              $scope.datasetNetwork = network.id;
            }).catch(function () {});
          }
          $scope.datasetProject = dataset['obiba.mica.HarmonizationDatasetDto.type'].project;
          $scope.datasetTable = dataset['obiba.mica.HarmonizationDatasetDto.type'].table;
        }
      };

      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};
      $scope.type = getTypeFromUrl();
      $scope.datasetId = $routeParams.id;
      $scope.activeTab = 0;
      $scope.dataset = DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);

      $scope.publish = function (publish) {
        if (publish) {
          DraftFileSystemSearchResource.searchUnderReview({path: '/' + $scope.type + '/' + $scope.dataset.id},
            function onSuccess(response) {
              DatasetPublicationResource.publish(
                {id: $scope.dataset.id, type: $scope.type, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
                function () {
                  $scope.dataset = DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
                });
            },
            function onError() {
              $log.error('Failed to search for Under Review files.');
            }
          );
        } else {
          DatasetPublicationResource.unPublish({id: $scope.dataset.id, type: $scope.type}, function () {
            $scope.dataset = DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
          });
        }
      };

      $scope.toStatus = function (value) {
        DraftDatasetStatusResource.toStatus({id: $scope.dataset.id, type:$scope.type, value: value}, function () {
          $scope.dataset = DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
        });
      };

      $scope.delete = function () {
        $scope.dataset.type = $scope.type;
        DatasetService.deleteDataset($scope.dataset, function () {
          $location.path('/' + $scope.type);
        }, $scope.tabs[$scope.activeTab].lang);
      };

      var getViewMode = function() {
        var result = /\/(revision[s\/]*|files|permissions)/.exec($location.path());
        if (result && result.length > 1) {
          switch (result[1]) {
            case 'revision':
            case 'revisions':
              return $scope.Mode.Revision;
            case 'files':
              return $scope.Mode.File;
            case 'permissions':
              return $scope.Mode.Permission;
            case 'comments':
              return $scope.Mode.Comment;
          }
        }

        return $scope.Mode.View;
      };

      $scope.viewMode = getViewMode();

      var viewRevision = function (datasetId, commitInfo) {
        $scope.commitInfo = commitInfo;
        $scope.dataset = DraftDatasetViewRevisionResource.view({
          id: datasetId,
          commitId: commitInfo.commitId,
          type: $scope.type
        });
      };

      var fetchDataset = function (datasetId) {
        $scope.dataset = DatasetResource.get({id: datasetId, type: $scope.type}, initializeDataset);
      };

      var fetchRevisions = function (datasetId, onSuccess) {
        DraftDatasetRevisionsResource.query({id: datasetId, type: $scope.type}, function (response) {
          if (onSuccess) {
            onSuccess(response);
          }
        });
      };

      var restoreRevision = function (datasetId, commitInfo, onSuccess) {
        if (commitInfo && $scope.datasetId === datasetId) {
          var args = {commitId: commitInfo.commitId, restoreSuccessCallback: onSuccess};

          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'dataset.restore-dialog.title',
              messageKey: 'dataset.restore-dialog.message',
              messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
            }, args
          );
        }
      };

      var onRestore = function (event, args) {
        if (args.commitId) {
          DraftDatasetRestoreRevisionResource.restore({id: $scope.datasetId, commitId: args.commitId, type: $scope.type},
            function () {
              fetchDataset($routeParams.id);
              $scope.datasetId = $routeParams.id;
              if (args.restoreSuccessCallback) {
                args.restoreSuccessCallback();
              }
            });
        }
      };

      $scope.fetchDataset = fetchDataset;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onRestore);
    }])

  .controller('DatasetPermissionsController', ['$scope', '$routeParams', 'DraftDatasetPermissionsResource', 'DraftDatasetAccessesResource',
    function ($scope, $routeParams, DraftDatasetPermissionsResource, DraftDatasetAccessesResource) {
    $scope.permissions = [];
    $scope.accesses = [];

    $scope.loadPermissions = function () {
      $scope.permissions = DraftDatasetPermissionsResource.query({id: $routeParams.id, datasetType: $scope.type});
      return $scope.permissions;
    };

    $scope.addPermission = function (permission) {
      return DraftDatasetPermissionsResource.save({id: $routeParams.id, datasetType: $scope.type}, permission);
    };

    $scope.deletePermission = function(permission) {
      return DraftDatasetPermissionsResource.delete({id: $routeParams.id, datasetType: $scope.type}, permission);
    };

    $scope.loadAccesses = function () {
      $scope.accesses = DraftDatasetAccessesResource.query({id: $routeParams.id, datasetType: $scope.type});
      return $scope.accesses;
    };

    $scope.deleteAccess = function (access) {
      return DraftDatasetAccessesResource.delete({id: $routeParams.id, datasetType: $scope.type}, access);
    };

    $scope.addAccess = function (access) {
      return DraftDatasetAccessesResource.save({id: $routeParams.id, datasetType: $scope.type}, access);
    };
  }])

  .controller('HarmonizationDatasetListController', ['$rootScope', '$scope', '$timeout', 'HarmonizationDatasetsResource', 'DatasetService',
    function ($rootScope, $scope, $timeout, HarmonizationDatasetsResource, DatasetService) {
      var onSuccess = function(response, responseHeaders) {
        $scope.harmonizedDatasets = response;
        $scope.loading = false;
        $scope.totalCount = parseInt(responseHeaders('X-Total-Count'), 10);
        $scope.loading = false;

        if (!$scope.hasDatasets) {
          $scope.hasDatasets = $scope.totalCount && !$scope.pagination.searchText;
        }
      };

      var onError = function() {
        $scope.loading = false;
      };

      $scope.deleteHarmonizationDataset = function (dataset) {
        DatasetService.deleteDataset(dataset, function () {
          refreshPage();
        });
      };

      $scope.pageChanged = function(page) {
        loadPage(page, $scope.pagination.searchText);
      };

      function refreshPage() {
        if($scope.pagination.current !== 1) {
          $scope.pagination.current = 1; //pageChanged event triggers reload
        } else {
          loadPage(1);
        }
      }

      function loadPage(page) {
        var data = {from:(page - 1) * $scope.limit, limit: $scope.limit};

        if($scope.pagination.searchText) {
          data.query = $scope.pagination.searchText + '*';
        }

        HarmonizationDatasetsResource.query(data, onSuccess, onError);
      }

      $scope.loading = true;
      $scope.hasStudies = false;
      $scope.pagination = {current: 1, searchText: ''};
      $scope.totalCount = 0;
      $scope.limit = 20;

      var currentSearch = null;

      $scope.$watch('pagination.searchText', function(newVal, oldVal) {
        if (!newVal && !oldVal) {
          return;
        }

        if(currentSearch) {
          $timeout.cancel(currentSearch);
        }

        currentSearch = $timeout(function() {
          refreshPage();
        }, 500);
      });

      loadPage($scope.pagination.current);
    }])

  .controller('HarmonizationDatasetViewController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    'HarmonizationDatasetResource',
    'HarmonizationDatasetPublicationResource',
    'MicaConfigResource',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              HarmonizationDatasetResource,
              HarmonizationDatasetPublicationResource,
              MicaConfigResource) {
      MicaConfigResource.get(function (micaConfig) {
        $scope.opal = micaConfig.opal;
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.dataset = HarmonizationDatasetResource.get({id: $routeParams.id}, function (dataset) {
        $scope.datasetProject = dataset['obiba.mica.HarmonizationDatasetDto.type'].project;
        $scope.datasetTable = dataset['obiba.mica.HarmonizationDatasetDto.type'].table;
      });

      $scope.activeTab = 0;

      $scope.isPublished = function () {
        return $scope.dataset.published;
      };

      $scope.publish = function () {
        if ($scope.dataset.published) {
          HarmonizationDatasetPublicationResource.unPublish({id: $scope.dataset.id}, function () {
            $scope.dataset = HarmonizationDatasetResource.get({id: $routeParams.id});
          });
        } else {
          HarmonizationDatasetPublicationResource.publish({id: $scope.dataset.id}, function () {
            $scope.dataset = HarmonizationDatasetResource.get({id: $routeParams.id});
          });
        }
      };

    }])

  .controller('StudyTableModalController', ['$scope', '$uibModalInstance', '$log', 'MicaConfigResource', 'StudyStatesResource', 'StudyStateProjectsResource', 'studyTable', 'tab',
    function ($scope, $uibModalInstance, $log, MicaConfigResource, StudyStatesResource, StudyStateProjectsResource, studyTable, tab) {
      $scope.studies = [];
      $scope.projects = [];
      $scope.selected = {};
      $scope.studyTable = $.extend(true, {}, studyTable);
      $scope.tab = tab;

      if (studyTable && studyTable !== {}) {
        $scope.selected.study = {
          id: studyTable.studyId,
          population: {
            id: studyTable.populationId,
            dataCollectionEvent: {id: studyTable.dataCollectionEventId}
          }
        };

        $scope.selected.project = {name: studyTable.project, table: studyTable.table};
      }

      StudyStatesResource.query().$promise.then(function (studies) {
        $scope.studies = studies.sort(function (a, b) { return a.id.localeCompare(b.id); });

        var selectedPopulation, selectedDce, selectedStudy = $scope.studies.filter(function (s) {return s.id === studyTable.studyId; })[0];

        if (selectedStudy) {
          $scope.selected.study = selectedStudy;
          selectedPopulation = selectedStudy.populationSummaries.filter(function (p) { return p.id === studyTable.populationId; })[0];

          if (selectedPopulation) {
            $scope.selected.study.population = selectedPopulation;

            selectedDce = selectedPopulation.dataCollectionEventSummaries.filter(function (dce) { return dce.id === studyTable.dataCollectionEventId; })[0];

            if (selectedDce) {
              $scope.selected.study.population.dataCollectionEvent = selectedDce;
            }
          }
        }
      });

      $scope.$watch('selected.study', function () {
        if ($scope.selected.study && $scope.selected.study.id) {
          StudyStateProjectsResource.query({id: $scope.selected.study.id}).$promise.then(function (projects) {
            $scope.projects = projects;
            var selectedTable, selectedProject = $scope.projects.filter(function (p) {return p.name === studyTable.project; })[0];

            if (selectedProject) {
              $scope.selected.project = selectedProject;

              selectedTable = selectedProject.datasource.table.filter(function (t) {return t === studyTable.table; })[0];

              if (selectedTable) {
                $scope.selected.project.table = selectedTable;
              }
            }

          });
        }
      });

      $scope.save = function (form) {
        if (form.$valid) {
          angular.extend($scope.studyTable, {
            studyId: $scope.selected.study.id,
            populationId: $scope.selected.study.population.id,
            dataCollectionEventId: $scope.selected.study.population.dataCollectionEvent.id,
            project: $scope.selected.project.name,
            table: $scope.selected.project.table
          });

          $uibModalInstance.close($scope.studyTable);
          return;
        }

        $scope.form = form;
        $scope.form.saveAttempted = true;
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

    }]);
