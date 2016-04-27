'use strict';

/* App Module */


var mica = angular.module('mica', [
  'angular-loading-bar',
  'http-auth-interceptor',
  'localytics.directives',
  'mica.config',
  'ngObiba',
  'mica.admin',
  'mica.network',
  'mica.study',
  'mica.dataset',
  'mica.dataAccesConfig',
  'mica.data-access-request',
  'mica.search',
  'ngCookies',
  'ngResource',
  'ngRoute',
  'pascalprecht.translate',
  'tmh.dynamicLocale',
  'xeditable',
  'matchMedia',
  'ngObibaMica',
  'ui.bootstrap',

]);

mica
  .provider('SessionProxy',
    function () {
      function Proxy() {
        var real;
        this.update = function (value) {
          real = value;
        };

        this.login = function() {
          return real.login;
        };

        this.roles = function() {
          return real.roles || [];
        };

        this.profile = function() {
          return real.profile;
        };
      }

      this.$get = function() {
        return new Proxy();
      };
    });

mica
  .config(['$routeProvider',
    '$httpProvider',
    '$translateProvider',
    'tmhDynamicLocaleProvider',
    'USER_ROLES',
    function ($routeProvider,
              $httpProvider,
              $translateProvider,
              tmhDynamicLocaleProvider,
              USER_ROLES) {


      $routeProvider
        .when('/login', {
          templateUrl: 'app/views/login.html',
          controller: 'LoginController',
          reloadOnSearch: false,
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .when('/error', {
          templateUrl: 'app/views/error.html',
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .when('/settings', {
          templateUrl: 'app/views/settings.html',
          controller: 'SettingsController',
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .when('/password', {
          templateUrl: 'app/views/password.html',
          controller: 'PasswordController',
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .when('/sessions', {
          templateUrl: 'app/views/sessions.html',
          controller: 'SessionsController',
          resolve: {
            resolvedSessions: ['Sessions', function (Sessions) {
              return Sessions.get();
            }]
          },
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .when('/audits', {
          templateUrl: 'app/views/audits.html',
          controller: 'AuditsController',
          access: {
            authorizedRoles: [USER_ROLES.admin]
          }
        })
        .when('/logout', {
          templateUrl: 'app/views/main.html',
          controller: 'LogoutController',
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .when('/docs', {
          templateUrl: 'app/views/docs.html',
          access: {
            authorizedRoles: [USER_ROLES.admin]
          }
        })
        .otherwise({
          templateUrl: 'app/views/main.html',
          controller: 'MainController',
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        });

      // Initialize angular-translate
      $translateProvider
        .useStaticFilesLoader({
          prefix: 'ws/config/i18n/',
          suffix: '.json'
        })
        .preferredLanguage('en')
        .fallbackLanguage('en')
        .useCookieStorage()
        .useSanitizeValueStrategy('escaped');

      tmhDynamicLocaleProvider.localeLocationPattern('bower_components/angular-i18n/angular-locale_{{locale}}.js');
      tmhDynamicLocaleProvider.useCookieStorage('NG_TRANSLATE_LANG_KEY');
    }])

  // Workaround for bug #1404
  // https://github.com/angular/angular.js/issues/1404
  // Source: http://plnkr.co/edit/hSMzWC?p=preview
  .config(['$provide', function ($provide) {
    $provide.decorator('ngModelDirective', ['$delegate', function ($delegate) {
      var ngModel = $delegate[0], controller = ngModel.controller;
      ngModel.controller = ['$scope', '$element', '$attrs', '$injector', function (scope, element, attrs, $injector) {
        var $interpolate = $injector.get('$interpolate');
        attrs.$set('name', $interpolate(attrs.name || '')(scope));
        $injector.invoke(controller, this, {
          '$scope': scope,
          '$element': element,
          '$attrs': attrs
        });
      }];
      return $delegate;
    }]);
    $provide.decorator('formDirective', ['$delegate', function ($delegate) {
      var form = $delegate[0], controller = form.controller;
      form.controller = ['$scope', '$element', '$attrs', '$injector', function (scope, element, attrs, $injector) {
        var $interpolate = $injector.get('$interpolate');
        attrs.$set('name', $interpolate(attrs.name || attrs.ngForm || '')(scope));
        $injector.invoke(controller, this, {
          '$scope': scope,
          '$element': element,
          '$attrs': attrs
        });
      }];
      return $delegate;
    }]);
  }])

  .run(['$rootScope',
    '$location',
    '$http',
    'AuthenticationSharedService',
    'Session',
    'USER_ROLES',
    'ServerErrorUtils',
    'UserProfileService',
    'editableOptions',

    function ($rootScope,
              $location,
              $http,
              AuthenticationSharedService,
              Session,
              USER_ROLES,
              ServerErrorUtils,
              UserProfileService,
              editableOptions) {
      $rootScope.$on('$routeChangeStart', function (event, next) {
        editableOptions.theme = 'bs3';

        $rootScope.authenticated = AuthenticationSharedService.isAuthenticated();
        $rootScope.hasRole = AuthenticationSharedService.isAuthorized;
        $rootScope.userRoles = USER_ROLES;
        $rootScope.subject = Session;
        $rootScope.userProfileService = UserProfileService;

        if (!$rootScope.authenticated) {
          if ('/login' !== $location.path()) {
            delete $rootScope.routeToLogin;
          }
          $rootScope.$broadcast('event:auth-loginRequired');
        } else if (!AuthenticationSharedService.isAuthorized(next.access ? next.access.authorizedRoles : '*')) {
          $rootScope.$broadcast('event:auth-notAuthorized');
        }
      });

      // Call when the the client is confirmed
      $rootScope.$on('event:auth-loginConfirmed', function () {
        delete $rootScope.routeToLogin;

        if ($location.path() === '/login') {
          var path = '/';
          var search = $location.search();
          if (search.hasOwnProperty('redirect')) {
            path = search.redirect;
            delete search.redirect;
          }
          $location.path(path).search(search).replace();
        }
      });

      function updateRedirect() {
        var path = $location.path();
        var invalidRedirectPaths = ['', '/error', '/logout', '/login'];
        if (invalidRedirectPaths.indexOf(path) === -1) {
          // save path to navigate to after login
          var search = $location.search();
          search.redirect = path;
          $location.search(search);
        }
      }

      function login() {
        if (!$rootScope.routeToLogin) {
          $rootScope.routeToLogin = true;
          updateRedirect();
        }
        $location.path('/login').replace();
      }

      $rootScope.$on('event:auth-loginRequired', function () {
        Session.destroy();
        login();
      });

      // Call when the 403 response is returned by the server
      $rootScope.$on('event:auth-notAuthorized', function () {
        if (!$rootScope.authenticated) {
          login();
        } else {
          $rootScope.errorMessage = 'errors.403';
          $location.path('/error').replace();
        }
      });

      $rootScope.$on('event:unhandled-server-error', function (event, response) {
        $rootScope.errorMessage = ServerErrorUtils.buildMessage(response);
        $location.path('/error').replace();
      });

      // Call when the user logs out
      $rootScope.$on('event:auth-loginCancelled', function () {
        $rootScope.authenticated = undefined;
        login();
      });
    }]);
