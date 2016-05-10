'use strict';

/* Services */

mica.factory('BrowserDetector', ['$window',
  function($window) {

    this.detect = function() {

      var userAgent = $window.navigator.userAgent;
      var browsers = {chrome: /chrome/i, safari: /safari/i, firefox: /firefox/i, ie: /internet explorer|mozilla.*windows nt/i};

      for(var key in browsers) {
        if (browsers[key].test(userAgent)) {
          return key;
        }
      }

      return 'unknown';
    };

    return this;
  }]);


mica.factory('CurrentSession', ['$resource',
  function ($resource) {
    return $resource('ws/auth/session/_current');
  }]);

mica.factory('UserProfile', ['$resource',
  function ($resource) {
    return $resource('ws/user/:id', {}, {
      'get': {method: 'GET', params: {id: '@id'}}
    });
  }]);

mica.factory('Account', ['$resource',
  function ($resource) {
    return $resource('ws/user/_current', {}, {
    });
  }]);

mica.factory('Password', ['$resource',
  function ($resource) {
    return $resource('ws/user/_current/password', {}, {
    });
  }]);

mica.factory('Session', ['SessionProxy','$cookieStore',
  function (SessionProxy, $cookieStore) {
    this.create = function (login, roles) {
      this.login = login;
      this.roles = roles;
      SessionProxy.update(this);
    };

    this.setProfile = function(profile) {
      this.profile = profile;
      SessionProxy.update(this);
    };

    this.destroy = function() {
      this.login = null;
      this.roles = null;
      this.profile = null;
      $cookieStore.remove('micasid');
      $cookieStore.remove('obibaid');
      SessionProxy.update(this);
    };

    return this;
  }]);

mica.service('AuthenticationSharedService', ['$rootScope', '$q', '$http', '$cookieStore', '$cookies', 'authService', 'Session', 'CurrentSession', 'UserProfile',
  function ($rootScope, $q, $http, $cookieStore, $cookies, authService, Session, CurrentSession, UserProfile) {
    var isInitializingSession = false, isInitializedDeferred = $q.defer(), self = this;

    this.isSessionInitialized = function() {
      return isInitializedDeferred.promise;
    };

    this.initSession = function() {
      var deferred = $q.defer();

      if(!isInitializingSession) {
        isInitializingSession = true;
        CurrentSession.get().$promise.then(function (data) {
          Session.create(data.username, data.roles);
          deferred.resolve(Session);
          authService.loginConfirmed(data);
          return data;
        }).catch(function() {
          deferred.reject();
          return $q.reject();
        }).then(function(data) {
          return UserProfile.get({id: data.username}).$promise;
        }).then(function(data) {
          Session.setProfile(data);
        }).finally(function() {
          isInitializingSession = false;
          isInitializedDeferred.resolve(true);
        });
      }

      return deferred.promise;
    };

    this.login = function (param) {
        $rootScope.authenticationError = false;
        var data = 'username=' + param.username + '&password=' + param.password;
        $http.post('ws/auth/sessions', data, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          ignoreAuthModule: 'ignoreAuthModule'
        }).success(function () {
          self.initSession();
        }).error(function() {
          $rootScope.authenticationError = true;
          Session.destroy();
        });
      };

    this.isAuthenticated = function () {
      return Session.login !== null && Session.login !== undefined;
    };

    this.isAuthorized = function (authorizedRoles) {
        if (!angular.isArray(authorizedRoles)) {
          if (authorizedRoles === '*') {
            return true;
          }

          authorizedRoles = [authorizedRoles];
        }

        var isAuthorized = false;

        angular.forEach(authorizedRoles, function (authorizedRole) {
          var authorized = (!!Session.login &&
            !angular.isUndefined(Session.roles) && Session.roles.indexOf(authorizedRole) !== -1);

          if (authorized || authorizedRole === '*') {
            isAuthorized = true;
          }
        });

        return isAuthorized;
      };

    this.logout = function () {
        $rootScope.authenticationError = false;
        $http({method: 'DELETE', url: 'ws/auth/session/_current', errorHandler: true})
          .success(function () {
            Session.destroy();
            authService.loginCancelled(null, 'logout');
          }).error(function () {
            Session.destroy();
            authService.loginCancelled(null, 'logout failure');
          }
        );
      };
  }]);

mica.factory('FormDirtyStateObserver', ['$uibModal',
  function ($uibModal) {
    return {
      observe: function(scope, $location) {
        var onLocationChangeOff = scope.$on('$locationChangeStart', function (event, newUrl) {
          if (scope.form.$dirty) {
            $uibModal.open({
              backdrop: 'static',
              controller: function ($scope, $uibModalInstance) {
                $scope.ok = function () { $uibModalInstance.close(true); };
                $scope.cancel = function () { $uibModalInstance.dismiss('cancel'); };
              },
              templateUrl: 'app/views/unsaved-modal.html'
            }).result.then(function (answer) {
              if (answer === true) {
                onLocationChangeOff();
                $location.path($location.url(newUrl).hash());
              }
            });

            event.preventDefault();
          }
        });
      }
    };

  }]);

mica.factory('MetricsService', ['$resource',
  function ($resource) {
    return $resource('jvm', {}, {
      'get': { method: 'GET'}
    });
  }]);

mica.factory('ThreadDumpService', ['$http',
  function ($http) {
    return {
      dump: function () {
        return $http.get('dump').then(function (response) {
          return response.data;
        });
      }
    };
  }]);

mica.factory('LogsService', ['$resource',
  function ($resource) {
    return $resource('ws/logs', {}, {
      'findAll': { method: 'GET', isArray: true},
      'changeLevel': { method: 'PUT'}
    });
  }]);

mica.factory('CacheService', ['$resource',
  function ($resource) {
    return {
      caches: $resource('ws/caches', {}, {
        'clear': {method: 'DELETE'}
      }),
      cache: $resource('ws/cache/:id', {id : '@id'}, {
        'clear': {method: 'DELETE'},
        'build': {method: 'PUT'}
      })
    };
  }]);

mica.factory('IndexService', ['$resource',
  function ($resource) {
    return {
      all: $resource('ws/config/_index', {}, {
        'build': {method: 'PUT'}
      }),
      networks: $resource('ws/draft/networks/_index', {}, {
        'build': {method: 'PUT'}
      }),
      studies: $resource('ws/draft/studies/_index', {}, {
        'build': {method: 'PUT'}
      }),
      datasets: $resource('ws/draft/datasets/_index', {}, {
        'build': {method: 'PUT'}
      }),
      studyDatasets: $resource('ws/draft/study-datasets/_index', {}, {
        'build': {method: 'PUT'}
      }),
      harmonizationDatasets: $resource('ws/draft/harmonization-datasets/_index', {}, {
        'build': {method: 'PUT'}
      })
    };
  }]);

mica.factory('AuditsService', ['$http',
  function ($http) {
    return {
      findAll: function () {
        return $http.get('ws/audits/all').then(function (response) {
          return response.data;
        });
      },
      findByDates: function (fromDate, toDate) {
        return $http.get('ws/audits/byDates', {params: {fromDate: fromDate, toDate: toDate}}).then(function (response) {
          return response.data;
        });
      }
    };
  }]);

mica.factory('MicaUtil', [function() {
  var generateNextId = function (ids) {
    var r = /[0-9]+$/, prevId, matches, newId, i = ids.length - 1;

    while (i > -1) {
      prevId = ids[i];
      matches = r.exec(prevId);

      if (matches && matches.length) {
        newId = prevId.replace(r, parseInt(matches[0], 10) + 1);
      } else {
        newId = prevId + '_1';
      }

      i = ids.indexOf(newId);
    }

    return newId;
  };

  return {
    generateNextId: function(ids) {
      return generateNextId(ids);
    }
  };
}]);

