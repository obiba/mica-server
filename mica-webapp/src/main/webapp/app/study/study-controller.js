/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.study

  .constant('STUDY_EVENTS', {
    studyUpdated: 'event:study-updated'
  })

  .controller('StudyMainController', ['$scope', '$location', 'StudyStatesResource',
    function($scope, $location, StudyStatesResource) {
      $scope.path = '#/individual-study';

      if($scope.micaConfig.isSingleStudyEnabled) {
        $scope.studies = StudyStatesResource.query({}, function(res) {
          if(res.length) {
            $location.path('/individual-study/' + res[0].id);
            $location.replace();
          }
        });
      }
  }])

  .controller('StudyListController', [
    '$scope', '$timeout', 'StudyStatesResource', 'DraftStudyDeleteService', 'AlertBuilder', mica.commons.ListController
  ])

  .controller('HarmonizationStudyListController', [
    '$scope', '$timeout', 'HarmonizationStudyStatesResource', 'DraftHarmonizationStudyDeleteService', 'AlertBuilder', mica.commons.ListController
  ])

  .controller('StudyViewController', [
    '$scope',
    '$rootScope',
    '$location',
    '$routeParams',
    '$translate',
    '$uibModal',
    '$timeout',
    '$filter',
    '$q',
    '$log',
    'NOTIFICATION_EVENTS',
    'CONTACT_EVENTS',
    'EntityFormResource',
    'LocalizedSchemaFormService',
    'MicaConfigResource',
    'DocumentPermissionsService',
    'StudyStatesResource',
    'DraftFileSystemSearchResource',
    'DraftStudyResource',
    'DraftStudyDeleteService',
    'DraftStudyRevisionsResource',
    'StudyUpdateWarningService',
    'EntityPathBuilder',
    mica.study.ViewController
  ])

  .controller('HarmonizationStudyViewController', [
    '$scope',
    '$rootScope',
    '$location',
    '$routeParams',
    '$translate',
    '$uibModal',
    '$timeout',
    '$filter',
    '$q',
    '$log',
    'NOTIFICATION_EVENTS',
    'CONTACT_EVENTS',
    'EntityFormResource',
    'LocalizedSchemaFormService',
    'MicaConfigResource',
    'DocumentPermissionsService',
    'HarmonizationStudyStatesResource',
    'DraftFileSystemSearchResource',
    'DraftHarmonizationStudyResource',
    'DraftHarmonizationStudyDeleteService',
    'DraftHarmonizationStudyRevisionsResource',
    'StudyUpdateWarningService',
    mica.study.HarmonizationStudyViewController
  ])

  .controller('StudyPermissionsController', [
    '$scope', '$routeParams', 'DraftStudyPermissionsResource', 'DraftStudyAccessesResource', mica.commons.PermissionsController
  ])

  .controller('HarmonizationStudyPermissionsController', [
    '$scope', '$routeParams', 'DraftHarmonizationStudyPermissionsResource', 'DraftHarmonizationStudyAccessesResource', mica.commons.PermissionsController
  ])

  .controller('StudyPopulationDceModalController', [
    '$scope',
    '$uibModalInstance',
    '$locale',
    '$location',
    '$translate',
    'lang',
    'dce',
    'sfOptions',
    'sfForm',
    'study',
    'path',
    'StudyTaxonomyService',
    'moment',
    function ($scope,
              $uibModalInstance,
              $locale,
              $location,
              $translate,
              lang,
              dce,
              sfOptions,
              sfForm,
              study,
              path,
              StudyTaxonomyService,
              moment) {
      $scope.months = moment.months();
      $scope.selectedLocale = lang;
      $scope.dce = dce;
      $scope.study = study;
      $scope.path = path;
      $scope.sfOptions = sfOptions;
      $scope.dceSfForm = sfForm;

      $scope.close = function () {
        $uibModalInstance.close();
      };

      $scope.viewFiles = function () {
        $uibModalInstance.close();
        $location.path(path.root).search({p: path.entity});
      };

      $scope.getTermLabels = function(vocabularyName, terms) {
        if (!terms) {
          return [];
        }

        var result = terms.map(function(term){
          return StudyTaxonomyService.getLabel(vocabularyName, term, $translate.use());
        });
        return result.join(', ');
      };
    }])

  .controller('StudyPopulationController', [
    '$scope',
    '$rootScope',
    '$routeParams',
    '$location',
    '$filter',
    '$translate',
    '$q',
    '$log',
    'MicaConfigResource',
    'SfOptionsService',
    'EntityFormResource',
    'DraftStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    'MicaUtil',
    mica.study.PopulationEditController
  ])

  .controller('HarmonizationStudyPopulationController', [
    '$scope',
    '$rootScope',
    '$routeParams',
    '$location',
    '$filter',
    '$translate',
    '$q',
    '$log',
    'MicaConfigResource',
    'SfOptionsService',
    'EntityFormResource',
    'DraftHarmonizationStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    'MicaUtil',
    mica.study.HarmonizationPopulationEditController
  ])

  .controller('StudyPopulationDceController', [
    '$scope',
    '$rootScope',
    '$routeParams',
    '$location',
    '$filter',
    '$translate',
    '$q',
    '$log',
    'MicaConfigResource',
    'SfOptionsService',
    'EntityFormResource',
    'DraftStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    'MicaUtil',
    mica.study.DataCollectionEventEditController
  ])

  .controller('StudyEditController', [
    '$scope',
    '$rootScope',
    '$routeParams',
    '$location',
    '$filter',
    '$translate',
    '$q',
    '$log',
    'MicaConfigResource',
    'SfOptionsService',
    'EntityFormResource',
    'DraftStudiesResource',
    'DraftStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    mica.study.EditController
  ])

  .controller('StudyImportController', [
    '$scope',
    '$http', 
    function ($scope, $http) {

      const CONNECTIONS_PARAMS_0 = 0;
      const DIFF_CUSTOM_FORM_1 = 1;
      const REMOTE_STUDIES_2 = 2;
      const OPERATIONS_SUMMARY_3 = 3;
      const FINISH_RESPONSE_MESSAGES_4 = 4;
      const HARMONIZATION_STUDY = 'harmonization-study';
      const INDIVIDUAL_STUDY = 'individual-study';
      
      const CONFIG_FORM_TITLE = {
    		'individual-study' : 'individual-study-config.individual-study-form-title',
		    'study-population' : 'individual-study-config.population-form-title',
		    'data-collection-event' : 'individual-study-config.data-collection-event-form-title',
		    'harmonization-study' : 'harmonization-study-config.harmonization-study-form-title',
		    'harmonization-study-population' : 'harmonization-study-config.harmonization-population-form-title'
         };

      $scope.modalIndex = CONNECTIONS_PARAMS_0;
      $scope.studyType = ($scope.path.indexOf('harmonization') > -1) ? HARMONIZATION_STUDY : INDIVIDUAL_STUDY;
      $scope.diffsCustomFormJSON = [];
      $scope.listDiffsForm = [];
      $scope.diffsConfigIsPossibleImport = false;

      //NEXT
      $scope.next = function() {

    	if ($scope.modalIndex === CONNECTIONS_PARAMS_0) {
    		console.log('[NEXT-0]');
        	
            $('body').css('cursor', 'progress');
            
            $http({
                url: 'ws/draft/studies/import/_differences',
                method: 'GET',
                params: {url: $scope.importVO.url, username: $scope.importVO.username, 
                         password: $scope.importVO.password, type: $scope.studyType}
            
              }).then(function(response) {
                console.log('[NEXT-0(response)]');

                var diffsEnum = response.data;
                
                $scope.diffsCustomFormJSON = [];
                $scope.listDiffsForm = [];

                for (var prop in diffsEnum) {
                	
	            	var jsonProp = JSON.parse(String(prop));
	        		
	        		$scope.diffsCustomFormJSON.push({
	          			formTitle : CONFIG_FORM_TITLE[jsonProp.formTitle], 
	      				endpoint : jsonProp.endpoint,
	      				isEqual :  diffsEnum[prop]
	      			  });
                	
	        		if (!diffsEnum[prop]) {
	        			$scope.listDiffsForm.push(jsonProp.formTitle);
	        		}
	        		
                	if ((jsonProp.formTitle === INDIVIDUAL_STUDY || jsonProp.formTitle === HARMONIZATION_STUDY) &&  diffsEnum[prop]) {
                		
                		$scope.diffsConfigIsPossibleImport = true;
                	}
                }

                $('#myModalDialog').addClass('modal-lg');
                $scope.modalIndex = DIFF_CUSTOM_FORM_1;
                $('body').css('cursor', 'default');
                
              });
            
    	} else if ($scope.modalIndex === DIFF_CUSTOM_FORM_1) {
    		console.log('[NEXT-1]');

            $scope.currentPage = 1;
            $scope.pageSize = 7;
            $('body').css('cursor', 'progress');

            $http({
                url: 'ws/draft/studies/import/_preview',
                method: 'GET',
                params: {url: $scope.importVO.url, username: $scope.importVO.username, 
                         password: $scope.importVO.password, type: $scope.studyType}
            
              }).then(function(response) {
                 console.log('[NEXT-1(response)]');
                 
                 $scope.studiesToImport = response.data;
            	 
                 $('body').css('cursor', 'default');
                 
                 $scope.modalIndex = REMOTE_STUDIES_2;
              });
            
        } else if ($scope.modalIndex === REMOTE_STUDIES_2) {
        	
        	console.log('[NEXT-2]');
        	
        	$scope.studiesToCreate = [];
        	$scope.studiesToUpdate = [];
        	$scope.studiesConflict = [];

        	var idStudiesToCheck = [];
        	
        	//check if studies exist locally
        	for (var i = 0; i < $scope.studiesToImport.length; i++) {
                if ( $scope.studiesToImport[i].checked ) {
                	
                	idStudiesToCheck.push($scope.studiesToImport[i].id);
                	$scope.studiesToCreate.push($scope.studiesToImport[i]);
                }
        	}
            
        	$http({
                url: 'ws/draft/studies/import/_summary',
                method: 'GET',
                params: {ids: idStudiesToCheck, 
                		 type: $scope.studyType }

              }).then(function(response) {
        		
                console.log('[NEXT-2(response)]');
               
                var respEnum = response.data;
                var resp = [];
                
                for (var prop in respEnum) {
                	resp.push(prop);
                }                	

            	for (var j in $scope.studiesToCreate) {

            		if (respEnum[ $scope.studiesToCreate[j].id] ) {

            			$scope.studiesConflict.push($scope.studiesToCreate[j]);
            			
            			delete $scope.studiesToCreate[j];
            			
            		} else if (resp.includes($scope.studiesToCreate[j].id)) {
            			
            			$scope.studiesToUpdate.push($scope.studiesToCreate[j]);
            			
            			delete $scope.studiesToCreate[j];
            		} 
            	}
            	
            	$scope.studiesToCreate = $scope.studiesToCreate.filter(function (el) { return el !== null; });
            	
            	$scope.modalIndex = OPERATIONS_SUMMARY_3;      	
            });
        }
      };

      //PREVIOUS
      $scope.previous = function() {

        if ($scope.modalIndex === DIFF_CUSTOM_FORM_1) {
        	console.log('[PREVIOUS-1]');
        	
        	$('#myModalDialog').removeClass('modal-lg');
        	$scope.modalIndex = CONNECTIONS_PARAMS_0;
            
        } else if ($scope.modalIndex === REMOTE_STUDIES_2) {
        	console.log('[PREVIOUS-2]');
        	
        	$scope.modalIndex = DIFF_CUSTOM_FORM_1;
        	
        } else if ($scope.modalIndex === OPERATIONS_SUMMARY_3) {
        	console.log('[PREVIOUS-3]');
        	
        	$scope.modalIndex = REMOTE_STUDIES_2;
        	
        } else if ($scope.modalIndex === FINISH_RESPONSE_MESSAGES_4) {
        	console.log('[PREVIOUS-4]');
        	
        	$scope.modalIndex = DIFF_CUSTOM_FORM_1;
        }
      };

      //CLICK_CHECKBOX
      $scope.clickCheckBox = function() {
		console.log('[CLICK_CHECKBOX]');
		
		for (var i = 0; i < $scope.studiesToImport.length; i++) {	
		    if ($scope.studiesToImport[i].checked) {
		    	$scope.studiesToImport.checked = true;
		    	return;
		    }
		}
		
		$scope.studiesToImport.checked = false;
      };
      
      //REMOVE_FROM_LIST
      $scope.removeFromList = function(studySummary, isInCreateList) {	  
		console.log('[REMOVE_FROM_LIST]');
		console.log(studySummary);
		
		var newDataList = [];

        angular.forEach(isInCreateList ? $scope.studiesToCreate : $scope.studiesToUpdate, function(v) {
	        if (v.id !== studySummary.id) {
	            newDataList.push(v);
	        }
        });    
        
        if (isInCreateList) {
        	$scope.studiesToCreate = newDataList;
        } else {
        	$scope.studiesToUpdate = newDataList;
        }
        
        angular.forEach($scope.studiesConflict, function(v) {
	        if (v.id === studySummary.id) {
	        	delete $scope.studiesConflict[v];
	        }
        }); 
        
        $scope.studiesConflict = $scope.studiesConflict.filter(function (el) { return el !== null; });
        
        if ($scope.studiesToCreate.length === 0 && $scope.studiesToUpdate.length === 0) {
        	$scope.modalIndex = REMOTE_STUDIES_2;
        }

      };

      
      //CLOSE
      $scope.close = function() {	  
  		console.log('[CLOSE]');
      };
      
      
      //FINISH
      $scope.finish = function() {
        console.log('[FINISH]');

        var idsToSave = [];
        $scope.studiesSaved = [];
        $scope.statusErrorImport = '';
        
        
        $('body').css('cursor', 'progress');
        
        for (var i in $scope.studiesToCreate) {
        	idsToSave.push( $scope.studiesToCreate[i].id );
        }
        
        for (var j in $scope.studiesToUpdate) {
        	idsToSave.push( $scope.studiesToUpdate[j].id );
        }
        
        if (idsToSave.length > 0) {

	        $http({
	          url: 'ws/draft/studies/import/_save',
	          method: 'PUT',
	          params: {url: $scope.importVO.url, 
	                   username: $scope.importVO.username, 
	                   password: $scope.importVO.password, 
	                   type: $scope.studyType,
	                   ids: idsToSave,
	                   listDiffsForm: $scope.listDiffsForm
	                   }
	
	        }).then(function(response) {
	          console.log('[FINISH(response)]');
	          
	          if (response.status !== 200) {
	        	  $scope.statusErrorImport += (response.status + ' ');
	          }
	          
	          $scope.idsSaved = response.data;
	          
	          angular.forEach($scope.studiesToUpdate, function(v) {
	  	        if ($scope.idsSaved.includes(v.id)) {
	  	        	$scope.studiesSaved.push(v);
	  	        }
	          }); 
	          
	          angular.forEach($scope.studiesToCreate, function(v) {
	        	if ($scope.idsSaved.includes(v.id)) {
	        		$scope.studiesSaved.push(v);
	        	}
		      });
	          
		    
	          $scope.studiesToUpdate = [];
	          $scope.studiesToCreate = [];
	          $scope.studiesConflict = [];
	          
	          $('body').css('cursor', 'default');
	          $scope.modalIndex = FINISH_RESPONSE_MESSAGES_4;
	         
	        });
        }
      };
  }])


  .controller('HarmonizationStudyEditController', [
    '$scope',
    '$rootScope',
    '$routeParams',
    '$location',
    '$filter',
    '$translate',
    '$q',
    '$log',
    'MicaConfigResource',
    'SfOptionsService',
    'EntityFormResource',
    'DraftHarmonizationStudiesResource',
    'DraftHarmonizationStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    mica.study.HarmonizationStudyEditController
  ]);
