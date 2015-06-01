/* global document */

'use strict';

mica.constant('USER_ROLES', {
  all: '*',
  admin: 'mica-administrator',
  reviewer: 'mica-reviewer',
  editor: 'mica-editor',
  user: 'mica-user',
  dao: 'mica-data-access-officer'
});

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

mica.factory('Session', ['$cookieStore',
  function ($cookieStore) {
    this.create = function (login, role) {
      this.login = login;
      this.role = role;
    };
    this.destroy = function () {
      this.login = null;
      this.role = null;
      $cookieStore.remove('mica_subject');
      $cookieStore.remove('micasid');
      $cookieStore.remove('obibaid');
    };
    return this;
  }]);

mica.factory('AuthenticationSharedService', ['$rootScope', '$http', '$cookieStore', '$cookies', 'authService', 'Session', 'CurrentSession',
  function ($rootScope, $http, $cookieStore, $cookies, authService, Session, CurrentSession) {
    return {
      login: function (param) {
        $rootScope.authenticationError = false;
        var data = 'username=' + param.username + '&password=' + param.password;
        $http.post('ws/auth/sessions', data, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          ignoreAuthModule: 'ignoreAuthModule'
        }).success(function () {
          CurrentSession.get(function (data) {
            Session.create(data.username, data.role);
            $cookieStore.put('mica_subject', JSON.stringify(Session));
            authService.loginConfirmed(data);
          });
        }).error(function () {
          $rootScope.authenticationError = true;
          Session.destroy();
        });
      },
      isAuthenticated: function () {
        // WORKAROUND: until next angular update, cookieStore is currently buggy
        function getMicaSidCookie() {
          var regexp = /micasid=([^;]+)/g;
          var result = regexp.exec(document.cookie);
          return (result === null) ? null : result[1];
        }

        if (!getMicaSidCookie()) {
          // session has terminated, cleanup
          Session.destroy();
          return false;
        }

        // check for Session object state
        if (!Session.login) {
          // check if there is a cookie for the subject
          var subjectCookie = $cookieStore.get('mica_subject');
          if (subjectCookie !== null && subjectCookie) {
            var account = JSON.parse($cookieStore.get('mica_subject'));
            Session.create(account.login, account.role);
            $rootScope.account = Session;
            return true;
          }
          // check if there is a Obiba session
          var obibaCookie = $cookies.obibaid;
          if (obibaCookie !== null && obibaCookie) {
            CurrentSession.get(function (data) {
              Session.create(data.username, data.role);
              $cookieStore.put('mica_subject', JSON.stringify(Session));
              authService.loginConfirmed(data);
            });
          }
        }
        return !!Session.login;
      },
      isAuthorized: function (authorizedRoles) {
        if (!angular.isArray(authorizedRoles)) {
          if (authorizedRoles === '*') {
            return true;
          }

          authorizedRoles = [authorizedRoles];
        }

        var isAuthorized = false;

        angular.forEach(authorizedRoles, function (authorizedRole) {
          var authorized = (!!Session.login &&
            Session.role === authorizedRole);

          if (authorized || authorizedRole === '*') {
            isAuthorized = true;
          }
        });

        return isAuthorized;
      },
      logout: function () {
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
      }
    };
  }]);

mica.factory('MetricsService', ['$resource',
  function ($resource) {
    return $resource('metrics/metrics', {}, {
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

mica.factory('HealthCheckService', ['$rootScope', '$http',
  function ($rootScope, $http) {
    return {
      check: function () {
        return $http.get('health').then(function (response) {
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
  var generateNextId = function(prevId) {
    var r= /[0-9]+$/,
      matches = r.exec(prevId);

    if (matches && matches.length) {
      return prevId.replace(r, parseInt(matches[0], 10) + 1);
    }

    return prevId ? prevId + '_1' : '';
  };

  return {
    generateNextId: function(prevId) {
      if(angular.isArray(prevId)) {
        var res = [];

        for(var i = 0; i < prevId.length; i++) {
          res[i] = {lang: prevId[i].lang, value: generateNextId(prevId[i].value)};
        }

        return res;
      }

      return generateNextId(prevId);
    }
  };
}]);

mica.constant('MicaConstants', {
  COUNTRIES_ISO_CODES: {
    'en': [
      {
        'code': 'AF',
        'name': 'Afghanistan'
      },
      {
        'code': 'AX',
        'name': 'Åland Islands'
      },
      {
        'code': 'AL',
        'name': 'Albania'
      },
      {
        'code': 'DZ',
        'name': 'Algeria'
      },
      {
        'code': 'AS',
        'name': 'American Samoa'
      },
      {
        'code': 'AD',
        'name': 'Andorra'
      },
      {
        'code': 'AO',
        'name': 'Angola'
      },
      {
        'code': 'AI',
        'name': 'Anguilla'
      },
      {
        'code': 'AQ',
        'name': 'Antarctica'
      },
      {
        'code': 'AG',
        'name': 'Antigua and Barbuda'
      },
      {
        'code': 'AR',
        'name': 'Argentina'
      },
      {
        'code': 'AM',
        'name': 'Armenia'
      },
      {
        'code': 'AW',
        'name': 'Aruba'
      },
      {
        'code': 'AU',
        'name': 'Australia'
      },
      {
        'code': 'AT',
        'name': 'Austria'
      },
      {
        'code': 'AZ',
        'name': 'Azerbaijan'
      },
      {
        'code': 'BS',
        'name': 'Bahamas'
      },
      {
        'code': 'BH',
        'name': 'Bahrain'
      },
      {
        'code': 'BD',
        'name': 'Bangladesh'
      },
      {
        'code': 'BB',
        'name': 'Barbados'
      },
      {
        'code': 'BY',
        'name': 'Belarus'
      },
      {
        'code': 'BE',
        'name': 'Belgium'
      },
      {
        'code': 'BZ',
        'name': 'Belize'
      },
      {
        'code': 'BJ',
        'name': 'Benin'
      },
      {
        'code': 'BM',
        'name': 'Bermuda'
      },
      {
        'code': 'BT',
        'name': 'Bhutan'
      },
      {
        'code': 'BO',
        'name': 'Bolivia (Plurinational State of)'
      },
      {
        'code': 'BQ',
        'name': 'Bonaire, Sint Eustatius and Saba'
      },
      {
        'code': 'BA',
        'name': 'Bosnia and Herzegovina'
      },
      {
        'code': 'BW',
        'name': 'Botswana'
      },
      {
        'code': 'BV',
        'name': 'Bouvet Island'
      },
      {
        'code': 'BR',
        'name': 'Brazil'
      },
      {
        'code': 'IO',
        'name': 'British Indian Ocean Territory'
      },
      {
        'code': 'BN',
        'name': 'Brunei Darussalam'
      },
      {
        'code': 'BG',
        'name': 'Bulgaria'
      },
      {
        'code': 'BF',
        'name': 'Burkina Faso'
      },
      {
        'code': 'BI',
        'name': 'Burundi'
      },
      {
        'code': 'KH',
        'name': 'Cambodia'
      },
      {
        'code': 'CM',
        'name': 'Cameroon'
      },
      {
        'code': 'CA',
        'name': 'Canada'
      },
      {
        'code': 'CV',
        'name': 'Cabo Verde'
      },
      {
        'code': 'KY',
        'name': 'Cayman Islands'
      },
      {
        'code': 'CF',
        'name': 'Central African Republic'
      },
      {
        'code': 'TD',
        'name': 'Chad'
      },
      {
        'code': 'CL',
        'name': 'Chile'
      },
      {
        'code': 'CN',
        'name': 'China'
      },
      {
        'code': 'CX',
        'name': 'Christmas Island'
      },
      {
        'code': 'CC',
        'name': 'Cocos (Keeling) Islands'
      },
      {
        'code': 'CO',
        'name': 'Colombia'
      },
      {
        'code': 'KM',
        'name': 'Comoros'
      },
      {
        'code': 'CG',
        'name': 'Congo'
      },
      {
        'code': 'CD',
        'name': 'Congo (Democratic Republic of the)'
      },
      {
        'code': 'CK',
        'name': 'Cook Islands'
      },
      {
        'code': 'CR',
        'name': 'Costa Rica'
      },
      {
        'code': 'CI',
        'name': 'Côte d\'Ivoire'
      },
      {
        'code': 'HR',
        'name': 'Croatia'
      },
      {
        'code': 'CU',
        'name': 'Cuba'
      },
      {
        'code': 'CW',
        'name': 'Curaçao'
      },
      {
        'code': 'CY',
        'name': 'Cyprus'
      },
      {
        'code': 'CZ',
        'name': 'Czech Republic'
      },
      {
        'code': 'DK',
        'name': 'Denmark'
      },
      {
        'code': 'DJ',
        'name': 'Djibouti'
      },
      {
        'code': 'DM',
        'name': 'Dominica'
      },
      {
        'code': 'DO',
        'name': 'Dominican Republic'
      },
      {
        'code': 'EC',
        'name': 'Ecuador'
      },
      {
        'code': 'EG',
        'name': 'Egypt'
      },
      {
        'code': 'SV',
        'name': 'El Salvador'
      },
      {
        'code': 'GQ',
        'name': 'Equatorial Guinea'
      },
      {
        'code': 'ER',
        'name': 'Eritrea'
      },
      {
        'code': 'EE',
        'name': 'Estonia'
      },
      {
        'code': 'ET',
        'name': 'Ethiopia'
      },
      {
        'code': 'FK',
        'name': 'Falkland Islands (Malvinas)'
      },
      {
        'code': 'FO',
        'name': 'Faroe Islands'
      },
      {
        'code': 'FJ',
        'name': 'Fiji'
      },
      {
        'code': 'FI',
        'name': 'Finland'
      },
      {
        'code': 'FR',
        'name': 'France'
      },
      {
        'code': 'GF',
        'name': 'French Guiana'
      },
      {
        'code': 'PF',
        'name': 'French Polynesia'
      },
      {
        'code': 'TF',
        'name': 'French Southern Territories'
      },
      {
        'code': 'GA',
        'name': 'Gabon'
      },
      {
        'code': 'GM',
        'name': 'Gambia'
      },
      {
        'code': 'GE',
        'name': 'Georgia'
      },
      {
        'code': 'DE',
        'name': 'Germany'
      },
      {
        'code': 'GH',
        'name': 'Ghana'
      },
      {
        'code': 'GI',
        'name': 'Gibraltar'
      },
      {
        'code': 'GR',
        'name': 'Greece'
      },
      {
        'code': 'GL',
        'name': 'Greenland'
      },
      {
        'code': 'GD',
        'name': 'Grenada'
      },
      {
        'code': 'GP',
        'name': 'Guadeloupe'
      },
      {
        'code': 'GU',
        'name': 'Guam'
      },
      {
        'code': 'GT',
        'name': 'Guatemala'
      },
      {
        'code': 'GG',
        'name': 'Guernsey'
      },
      {
        'code': 'GN',
        'name': 'Guinea'
      },
      {
        'code': 'GW',
        'name': 'Guinea-Bissau'
      },
      {
        'code': 'GY',
        'name': 'Guyana'
      },
      {
        'code': 'HT',
        'name': 'Haiti'
      },
      {
        'code': 'HM',
        'name': 'Heard Island and McDonald Islands'
      },
      {
        'code': 'VA',
        'name': 'Holy See'
      },
      {
        'code': 'HN',
        'name': 'Honduras'
      },
      {
        'code': 'HK',
        'name': 'Hong Kong'
      },
      {
        'code': 'HU',
        'name': 'Hungary'
      },
      {
        'code': 'IS',
        'name': 'Iceland'
      },
      {
        'code': 'IN',
        'name': 'India'
      },
      {
        'code': 'ID',
        'name': 'Indonesia'
      },
      {
        'code': 'IR',
        'name': 'Iran (Islamic Republic of)'
      },
      {
        'code': 'IQ',
        'name': 'Iraq'
      },
      {
        'code': 'IE',
        'name': 'Ireland'
      },
      {
        'code': 'IM',
        'name': 'Isle of Man'
      },
      {
        'code': 'IL',
        'name': 'Israel'
      },
      {
        'code': 'IT',
        'name': 'Italy'
      },
      {
        'code': 'JM',
        'name': 'Jamaica'
      },
      {
        'code': 'JP',
        'name': 'Japan'
      },
      {
        'code': 'JE',
        'name': 'Jersey'
      },
      {
        'code': 'JO',
        'name': 'Jordan'
      },
      {
        'code': 'KZ',
        'name': 'Kazakhstan'
      },
      {
        'code': 'KE',
        'name': 'Kenya'
      },
      {
        'code': 'KI',
        'name': 'Kiribati'
      },
      {
        'code': 'KP',
        'name': 'Korea (Democratic People\'s Republic of)'
      },
      {
        'code': 'KR',
        'name': 'Korea (Republic of)'
      },
      {
        'code': 'KW',
        'name': 'Kuwait'
      },
      {
        'code': 'KG',
        'name': 'Kyrgyzstan'
      },
      {
        'code': 'LA',
        'name': 'Lao People\'s Democratic Republic'
      },
      {
        'code': 'LV',
        'name': 'Latvia'
      },
      {
        'code': 'LB',
        'name': 'Lebanon'
      },
      {
        'code': 'LS',
        'name': 'Lesotho'
      },
      {
        'code': 'LR',
        'name': 'Liberia'
      },
      {
        'code': 'LY',
        'name': 'Libya'
      },
      {
        'code': 'LI',
        'name': 'Liechtenstein'
      },
      {
        'code': 'LT',
        'name': 'Lithuania'
      },
      {
        'code': 'LU',
        'name': 'Luxembourg'
      },
      {
        'code': 'MO',
        'name': 'Macao'
      },
      {
        'code': 'MK',
        'name': 'Macedonia (the former Yugoslav Republic of)'
      },
      {
        'code': 'MG',
        'name': 'Madagascar'
      },
      {
        'code': 'MW',
        'name': 'Malawi'
      },
      {
        'code': 'MY',
        'name': 'Malaysia'
      },
      {
        'code': 'MV',
        'name': 'Maldives'
      },
      {
        'code': 'ML',
        'name': 'Mali'
      },
      {
        'code': 'MT',
        'name': 'Malta'
      },
      {
        'code': 'MH',
        'name': 'Marshall Islands'
      },
      {
        'code': 'MQ',
        'name': 'Martinique'
      },
      {
        'code': 'MR',
        'name': 'Mauritania'
      },
      {
        'code': 'MU',
        'name': 'Mauritius'
      },
      {
        'code': 'YT',
        'name': 'Mayotte'
      },
      {
        'code': 'MX',
        'name': 'Mexico'
      },
      {
        'code': 'FM',
        'name': 'Micronesia (Federated States of)'
      },
      {
        'code': 'MD',
        'name': 'Moldova (Republic of)'
      },
      {
        'code': 'MC',
        'name': 'Monaco'
      },
      {
        'code': 'MN',
        'name': 'Mongolia'
      },
      {
        'code': 'ME',
        'name': 'Montenegro'
      },
      {
        'code': 'MS',
        'name': 'Montserrat'
      },
      {
        'code': 'MA',
        'name': 'Morocco'
      },
      {
        'code': 'MZ',
        'name': 'Mozambique'
      },
      {
        'code': 'MM',
        'name': 'Myanmar'
      },
      {
        'code': 'NA',
        'name': 'Namibia'
      },
      {
        'code': 'NR',
        'name': 'Nauru'
      },
      {
        'code': 'NP',
        'name': 'Nepal'
      },
      {
        'code': 'NL',
        'name': 'Netherlands'
      },
      {
        'code': 'NC',
        'name': 'New Caledonia'
      },
      {
        'code': 'NZ',
        'name': 'New Zealand'
      },
      {
        'code': 'NI',
        'name': 'Nicaragua'
      },
      {
        'code': 'NE',
        'name': 'Niger'
      },
      {
        'code': 'NG',
        'name': 'Nigeria'
      },
      {
        'code': 'NU',
        'name': 'Niue'
      },
      {
        'code': 'NF',
        'name': 'Norfolk Island'
      },
      {
        'code': 'MP',
        'name': 'Northern Mariana Islands'
      },
      {
        'code': 'NO',
        'name': 'Norway'
      },
      {
        'code': 'OM',
        'name': 'Oman'
      },
      {
        'code': 'PK',
        'name': 'Pakistan'
      },
      {
        'code': 'PW',
        'name': 'Palau'
      },
      {
        'code': 'PS',
        'name': 'Palestine, State of'
      },
      {
        'code': 'PA',
        'name': 'Panama'
      },
      {
        'code': 'PG',
        'name': 'Papua New Guinea'
      },
      {
        'code': 'PY',
        'name': 'Paraguay'
      },
      {
        'code': 'PE',
        'name': 'Peru'
      },
      {
        'code': 'PH',
        'name': 'Philippines'
      },
      {
        'code': 'PN',
        'name': 'Pitcairn'
      },
      {
        'code': 'PL',
        'name': 'Poland'
      },
      {
        'code': 'PT',
        'name': 'Portugal'
      },
      {
        'code': 'PR',
        'name': 'Puerto Rico'
      },
      {
        'code': 'QA',
        'name': 'Qatar'
      },
      {
        'code': 'RE',
        'name': 'Réunion'
      },
      {
        'code': 'RO',
        'name': 'Romania'
      },
      {
        'code': 'RU',
        'name': 'Russian Federation'
      },
      {
        'code': 'RW',
        'name': 'Rwanda'
      },
      {
        'code': 'BL',
        'name': 'Saint Barthélemy'
      },
      {
        'code': 'SH',
        'name': 'Saint Helena, Ascension and Tristan da Cunha'
      },
      {
        'code': 'KN',
        'name': 'Saint Kitts and Nevis'
      },
      {
        'code': 'LC',
        'name': 'Saint Lucia'
      },
      {
        'code': 'MF',
        'name': 'Saint Martin (French part)'
      },
      {
        'code': 'PM',
        'name': 'Saint Pierre and Miquelon'
      },
      {
        'code': 'VC',
        'name': 'Saint Vincent and the Grenadines'
      },
      {
        'code': 'WS',
        'name': 'Samoa'
      },
      {
        'code': 'SM',
        'name': 'San Marino'
      },
      {
        'code': 'ST',
        'name': 'Sao Tome and Principe'
      },
      {
        'code': 'SA',
        'name': 'Saudi Arabia'
      },
      {
        'code': 'SN',
        'name': 'Senegal'
      },
      {
        'code': 'RS',
        'name': 'Serbia'
      },
      {
        'code': 'SC',
        'name': 'Seychelles'
      },
      {
        'code': 'SL',
        'name': 'Sierra Leone'
      },
      {
        'code': 'SG',
        'name': 'Singapore'
      },
      {
        'code': 'SX',
        'name': 'Sint Maarten (Dutch part)'
      },
      {
        'code': 'SK',
        'name': 'Slovakia'
      },
      {
        'code': 'SI',
        'name': 'Slovenia'
      },
      {
        'code': 'SB',
        'name': 'Solomon Islands'
      },
      {
        'code': 'SO',
        'name': 'Somalia'
      },
      {
        'code': 'ZA',
        'name': 'South Africa'
      },
      {
        'code': 'GS',
        'name': 'South Georgia and the South Sandwich Islands'
      },
      {
        'code': 'SS',
        'name': 'South Sudan'
      },
      {
        'code': 'ES',
        'name': 'Spain'
      },
      {
        'code': 'LK',
        'name': 'Sri Lanka'
      },
      {
        'code': 'SD',
        'name': 'Sudan'
      },
      {
        'code': 'SR',
        'name': 'Suriname'
      },
      {
        'code': 'SJ',
        'name': 'Svalbard and Jan Mayen'
      },
      {
        'code': 'SZ',
        'name': 'Swaziland'
      },
      {
        'code': 'SE',
        'name': 'Sweden'
      },
      {
        'code': 'CH',
        'name': 'Switzerland'
      },
      {
        'code': 'SY',
        'name': 'Syrian Arab Republic'
      },
      {
        'code': 'TW',
        'name': 'Taiwan, Province of China'
      },
      {
        'code': 'TJ',
        'name': 'Tajikistan'
      },
      {
        'code': 'TZ',
        'name': 'Tanzania, United Republic of'
      },
      {
        'code': 'TH',
        'name': 'Thailand'
      },
      {
        'code': 'TL',
        'name': 'Timor-Leste'
      },
      {
        'code': 'TG',
        'name': 'Togo'
      },
      {
        'code': 'TK',
        'name': 'Tokelau'
      },
      {
        'code': 'TO',
        'name': 'Tonga'
      },
      {
        'code': 'TT',
        'name': 'Trinidad and Tobago'
      },
      {
        'code': 'TN',
        'name': 'Tunisia'
      },
      {
        'code': 'TR',
        'name': 'Turkey'
      },
      {
        'code': 'TM',
        'name': 'Turkmenistan'
      },
      {
        'code': 'TC',
        'name': 'Turks and Caicos Islands'
      },
      {
        'code': 'TV',
        'name': 'Tuvalu'
      },
      {
        'code': 'UG',
        'name': 'Uganda'
      },
      {
        'code': 'UA',
        'name': 'Ukraine'
      },
      {
        'code': 'AE',
        'name': 'United Arab Emirates'
      },
      {
        'code': 'GB',
        'name': 'United Kingdom of Great Britain and Northern Ireland'
      },
      {
        'code': 'US',
        'name': 'United States of America'
      },
      {
        'code': 'UM',
        'name': 'United States Minor Outlying Islands'
      },
      {
        'code': 'UY',
        'name': 'Uruguay'
      },
      {
        'code': 'UZ',
        'name': 'Uzbekistan'
      },
      {
        'code': 'VU',
        'name': 'Vanuatu'
      },
      {
        'code': 'VE',
        'name': 'Venezuela (Bolivarian Republic of)'
      },
      {
        'code': 'VN',
        'name': 'Viet Nam'
      },
      {
        'code': 'VG',
        'name': 'Virgin Islands (British)'
      },
      {
        'code': 'VI',
        'name': 'Virgin Islands (U.S.)'
      },
      {
        'code': 'WF',
        'name': 'Wallis and Futuna'
      },
      {
        'code': 'EH',
        'name': 'Western Sahara'
      },
      {
        'code': 'YE',
        'name': 'Yemen'
      },
      {
        'code': 'ZM',
        'name': 'Zambia'
      },
      {
        'code': 'ZW',
        'name': 'Zimbabwe'
      }
    ],
    'fr': [
      {
        'code': 'AF',
        'name': 'Afghanistan'
      },
      {
        'code': 'AX',
        'name': 'Albanie'
      },
      {
        'code': 'AL',
        'name': 'Antarctique'
      },
      {
        'code': 'DZ',
        'name': 'Algérie'
      },
      {
        'code': 'AS',
        'name': 'Samoa américaines'
      },
      {
        'code': 'AD',
        'name': 'Andorre'
      },
      {
        'code': 'AO',
        'name': 'Angola'
      },
      {
        'code': 'AI',
        'name': 'Antigua-et-Barbuda'
      },
      {
        'code': 'AQ',
        'name': 'Azerbaïdjan'
      },
      {
        'code': 'AG',
        'name': 'Argentine'
      },
      {
        'code': 'AR',
        'name': 'Australie'
      },
      {
        'code': 'AM',
        'name': 'Autriche'
      },
      {
        'code': 'AW',
        'name': 'Bahamas'
      },
      {
        'code': 'AU',
        'name': 'Bahreïn'
      },
      {
        'code': 'AT',
        'name': 'Bangladesh'
      },
      {
        'code': 'AZ',
        'name': 'Arménie'
      },
      {
        'code': 'BS',
        'name': 'Barbade'
      },
      {
        'code': 'BH',
        'name': 'Belgique'
      },
      {
        'code': 'BD',
        'name': 'Bermudes'
      },
      {
        'code': 'BB',
        'name': 'Bhoutan'
      },
      {
        'code': 'BY',
        'name': 'Bolivie'
      },
      {
        'code': 'BE',
        'name': 'Bosnie-Herzégovine'
      },
      {
        'code': 'BZ',
        'name': 'Botswana'
      },
      {
        'code': 'BJ',
        'name': 'Île Bouvet'
      },
      {
        'code': 'BM',
        'name': 'Brésil'
      },
      {
        'code': 'BT',
        'name': 'Belize'
      },
      {
        'code': 'BO',
        'name': 'Territoire britannique de l\'océan Indien'
      },
      {
        'code': 'BQ',
        'name': 'Salomon'
      },
      {
        'code': 'BA',
        'name': 'Îles Vierges britanniques'
      },
      {
        'code': 'BW',
        'name': 'Brunei'
      },
      {
        'code': 'BV',
        'name': 'Bulgarie'
      },
      {
        'code': 'BR',
        'name': 'Birmanie'
      },
      {
        'code': 'IO',
        'name': 'Burundi'
      },
      {
        'code': 'BN',
        'name': 'Biélorussie'
      },
      {
        'code': 'BG',
        'name': 'Cambodge'
      },
      {
        'code': 'BF',
        'name': 'Cameroun'
      },
      {
        'code': 'BI',
        'name': 'Canada'
      },
      {
        'code': 'KH',
        'name': 'Cap-Vert'
      },
      {
        'code': 'CM',
        'name': 'Îles Caïmans'
      },
      {
        'code': 'CA',
        'name': 'République centrafricaine'
      },
      {
        'code': 'CV',
        'name': 'Sri Lanka'
      },
      {
        'code': 'KY',
        'name': 'Tchad'
      },
      {
        'code': 'CF',
        'name': 'Chili'
      },
      {
        'code': 'TD',
        'name': 'Chine'
      },
      {
        'code': 'CL',
        'name': 'Taïwan / (République de Chine (Taïwan))'
      },
      {
        'code': 'CN',
        'name': 'Île Christmas'
      },
      {
        'code': 'CX',
        'name': 'Îles Cocos'
      },
      {
        'code': 'CC',
        'name': 'Colombie'
      },
      {
        'code': 'CO',
        'name': 'Comores (pays)'
      },
      {
        'code': 'KM',
        'name': 'Mayotte'
      },
      {
        'code': 'CG',
        'name': 'République du Congo'
      },
      {
        'code': 'CD',
        'name': 'République démocratique du Congo'
      },
      {
        'code': 'CK',
        'name': 'Îles Cook'
      },
      {
        'code': 'CR',
        'name': 'Costa Rica'
      },
      {
        'code': 'CI',
        'name': 'Croatie'
      },
      {
        'code': 'HR',
        'name': 'Cuba'
      },
      {
        'code': 'CU',
        'name': 'Chypre (pays)'
      },
      {
        'code': 'CW',
        'name': 'République tchèque'
      },
      {
        'code': 'CY',
        'name': 'Bénin'
      },
      {
        'code': 'CZ',
        'name': 'Danemark'
      },
      {
        'code': 'DK',
        'name': 'Dominique'
      },
      {
        'code': 'DJ',
        'name': 'République dominicaine'
      },
      {
        'code': 'DM',
        'name': 'Équateur (pays)'
      },
      {
        'code': 'DO',
        'name': 'Salvador'
      },
      {
        'code': 'EC',
        'name': 'Guinée équatoriale'
      },
      {
        'code': 'EG',
        'name': 'Éthiopie'
      },
      {
        'code': 'SV',
        'name': 'Érythrée'
      },
      {
        'code': 'GQ',
        'name': 'Estonie'
      },
      {
        'code': 'ER',
        'name': 'Îles Féroé'
      },
      {
        'code': 'EE',
        'name': 'Malouines'
      },
      {
        'code': 'ET',
        'name': 'Géorgie du Sud-et-les Îles Sandwich du Sud'
      },
      {
        'code': 'FK',
        'name': 'Fidji'
      },
      {
        'code': 'FO',
        'name': 'Finlande'
      },
      {
        'code': 'FJ',
        'name': 'Îles Åland'
      },
      {
        'code': 'FI',
        'name': 'France'
      },
      {
        'code': 'FR',
        'name': 'Guyane'
      },
      {
        'code': 'GF',
        'name': 'Polynésie française'
      },
      {
        'code': 'PF',
        'name': 'Terres australes et antarctiques françaises'
      },
      {
        'code': 'TF',
        'name': 'Djibouti'
      },
      {
        'code': 'GA',
        'name': 'Gabon'
      },
      {
        'code': 'GM',
        'name': 'Géorgie (pays)'
      },
      {
        'code': 'GE',
        'name': 'Gambie'
      },
      {
        'code': 'DE',
        'name': 'Palestine'
      },
      {
        'code': 'GH',
        'name': 'Allemagne'
      },
      {
        'code': 'GI',
        'name': 'Ghana'
      },
      {
        'code': 'GR',
        'name': 'Gibraltar'
      },
      {
        'code': 'GL',
        'name': 'Kiribati'
      },
      {
        'code': 'GD',
        'name': 'Grèce'
      },
      {
        'code': 'GP',
        'name': 'Groenland'
      },
      {
        'code': 'GU',
        'name': 'Grenade (pays)'
      },
      {
        'code': 'GT',
        'name': 'Guadeloupe'
      },
      {
        'code': 'GG',
        'name': 'Guam'
      },
      {
        'code': 'GN',
        'name': 'Guatemala'
      },
      {
        'code': 'GW',
        'name': 'Guinée'
      },
      {
        'code': 'GY',
        'name': 'Guyana'
      },
      {
        'code': 'HT',
        'name': 'Haïti'
      },
      {
        'code': 'HM',
        'name': 'Îles Heard-et-MacDonald'
      },
      {
        'code': 'VA',
        'name': 'Saint-Siège (État de la Cité du Vatican)'
      },
      {
        'code': 'HN',
        'name': 'Honduras'
      },
      {
        'code': 'HK',
        'name': 'Hong Kong'
      },
      {
        'code': 'HU',
        'name': 'Hongrie'
      },
      {
        'code': 'IS',
        'name': 'Islande'
      },
      {
        'code': 'IN',
        'name': 'Inde'
      },
      {
        'code': 'ID',
        'name': 'Indonésie'
      },
      {
        'code': 'IR',
        'name': 'Iran'
      },
      {
        'code': 'IQ',
        'name': 'Irak'
      },
      {
        'code': 'IE',
        'name': 'Irlande (pays)'
      },
      {
        'code': 'IM',
        'name': 'Israël'
      },
      {
        'code': 'IL',
        'name': 'Italie'
      },
      {
        'code': 'IT',
        'name': 'Côte d\'Ivoire'
      },
      {
        'code': 'JM',
        'name': 'Jamaïque'
      },
      {
        'code': 'JP',
        'name': 'Japon'
      },
      {
        'code': 'JE',
        'name': 'Kazakhstan'
      },
      {
        'code': 'JO',
        'name': 'Jordanie'
      },
      {
        'code': 'KZ',
        'name': 'Kenya'
      },
      {
        'code': 'KE',
        'name': 'Corée du Nord'
      },
      {
        'code': 'KI',
        'name': 'Corée du Sud'
      },
      {
        'code': 'KP',
        'name': 'Koweït'
      },
      {
        'code': 'KR',
        'name': 'Kirghizistan'
      },
      {
        'code': 'KW',
        'name': 'Laos'
      },
      {
        'code': 'KG',
        'name': 'Liban'
      },
      {
        'code': 'LA',
        'name': 'Lesotho'
      },
      {
        'code': 'LV',
        'name': 'Lettonie'
      },
      {
        'code': 'LB',
        'name': 'Liberia'
      },
      {
        'code': 'LS',
        'name': 'Libye'
      },
      {
        'code': 'LR',
        'name': 'Liechtenstein'
      },
      {
        'code': 'LY',
        'name': 'Lituanie'
      },
      {
        'code': 'LI',
        'name': 'Luxembourg (pays)'
      },
      {
        'code': 'LT',
        'name': 'Macao'
      },
      {
        'code': 'LU',
        'name': 'Madagascar'
      },
      {
        'code': 'MO',
        'name': 'Malawi'
      },
      {
        'code': 'MK',
        'name': 'Malaisie'
      },
      {
        'code': 'MG',
        'name': 'Maldives'
      },
      {
        'code': 'MW',
        'name': 'Mali'
      },
      {
        'code': 'MY',
        'name': 'Malte'
      },
      {
        'code': 'MV',
        'name': 'Martinique'
      },
      {
        'code': 'ML',
        'name': 'Mauritanie'
      },
      {
        'code': 'MT',
        'name': 'Maurice (pays)'
      },
      {
        'code': 'MH',
        'name': 'Mexique'
      },
      {
        'code': 'MQ',
        'name': 'Monaco'
      },
      {
        'code': 'MR',
        'name': 'Mongolie'
      },
      {
        'code': 'MU',
        'name': 'Moldavie'
      },
      {
        'code': 'YT',
        'name': 'Monténégro'
      },
      {
        'code': 'MX',
        'name': 'Montserrat'
      },
      {
        'code': 'FM',
        'name': 'Maroc'
      },
      {
        'code': 'MD',
        'name': 'Mozambique'
      },
      {
        'code': 'MC',
        'name': 'Oman'
      },
      {
        'code': 'MN',
        'name': 'Namibie'
      },
      {
        'code': 'ME',
        'name': 'Nauru'
      },
      {
        'code': 'MS',
        'name': 'Népal'
      },
      {
        'code': 'MA',
        'name': 'Pays-Bas'
      },
      {
        'code': 'MZ',
        'name': 'Curaçao'
      },
      {
        'code': 'MM',
        'name': 'Aruba'
      },
      {
        'code': 'NA',
        'name': 'Sint Maarten'
      },
      {
        'code': 'NR',
        'name': 'Pays-Bas caribéens'
      },
      {
        'code': 'NP',
        'name': '  Nouvelle-Calédonie'
      },
      {
        'code': 'NL',
        'name': 'Vanuatu'
      },
      {
        'code': 'NC',
        'name': 'Nouvelle-Zélande'
      },
      {
        'code': 'NZ',
        'name': 'Nicaragua'
      },
      {
        'code': 'NI',
        'name': 'Niger'
      },
      {
        'code': 'NE',
        'name': 'Nigeria'
      },
      {
        'code': 'NG',
        'name': 'Niue'
      },
      {
        'code': 'NU',
        'name': 'Île Norfolk'
      },
      {
        'code': 'NF',
        'name': 'Norvège'
      },
      {
        'code': 'MP',
        'name': 'Îles Mariannes du Nord'
      },
      {
        'code': 'NO',
        'name': 'Îles mineures éloignées des États-Unis'
      },
      {
        'code': 'OM',
        'name': 'Micronésie (pays)'
      },
      {
        'code': 'PK',
        'name': 'Îles Marshall (pays)'
      },
      {
        'code': 'PW',
        'name': 'Palaos'
      },
      {
        'code': 'PS',
        'name': 'Pakistan'
      },
      {
        'code': 'PA',
        'name': 'Panama'
      },
      {
        'code': 'PG',
        'name': 'Papouasie-Nouvelle-Guinée'
      },
      {
        'code': 'PY',
        'name': 'Paraguay'
      },
      {
        'code': 'PE',
        'name': 'Pérou'
      },
      {
        'code': 'PH',
        'name': 'Philippines'
      },
      {
        'code': 'PN',
        'name': 'Îles Pitcairn'
      },
      {
        'code': 'PL',
        'name': 'Pologne'
      },
      {
        'code': 'PT',
        'name': 'Portugal'
      },
      {
        'code': 'PR',
        'name': 'Guinée-Bissau'
      },
      {
        'code': 'QA',
        'name': 'Timor oriental'
      },
      {
        'code': 'RE',
        'name': 'Porto Rico'
      },
      {
        'code': 'RO',
        'name': 'Qatar'
      },
      {
        'code': 'RU',
        'name': 'La Réunion'
      },
      {
        'code': 'RW',
        'name': 'Roumanie'
      },
      {
        'code': 'BL',
        'name': 'Russie'
      },
      {
        'code': 'SH',
        'name': 'Rwanda'
      },
      {
        'code': 'KN',
        'name': 'Saint-Barthélemy'
      },
      {
        'code': 'LC',
        'name': 'Sainte-Hélène, Ascension et Tristan da Cunha'
      },
      {
        'code': 'MF',
        'name': 'Saint-Christophe-et-Niévès'
      },
      {
        'code': 'PM',
        'name': 'Anguilla'
      },
      {
        'code': 'VC',
        'name': 'Sainte-Lucie'
      },
      {
        'code': 'WS',
        'name': 'Saint-Martin'
      },
      {
        'code': 'SM',
        'name': 'Saint-Pierre-et-Miquelon'
      },
      {
        'code': 'ST',
        'name': 'Saint-Vincent-et-les Grenadines'
      },
      {
        'code': 'SA',
        'name': 'Saint-Marin'
      },
      {
        'code': 'SN',
        'name': 'Sao Tomé-et-Principe'
      },
      {
        'code': 'RS',
        'name': 'Arabie saoudite'
      },
      {
        'code': 'SC',
        'name': 'Sénégal'
      },
      {
        'code': 'SL',
        'name': 'Serbie'
      },
      {
        'code': 'SG',
        'name': 'Seychelles'
      },
      {
        'code': 'SX',
        'name': 'Sierra Leone'
      },
      {
        'code': 'SK',
        'name': 'Singapour'
      },
      {
        'code': 'SI',
        'name': 'Slovaquie'
      },
      {
        'code': 'SB',
        'name': 'Viêt Nam'
      },
      {
        'code': 'SO',
        'name': 'Slovénie'
      },
      {
        'code': 'ZA',
        'name': 'Somalie'
      },
      {
        'code': 'GS',
        'name': 'Afrique du Sud'
      },
      {
        'code': 'SS',
        'name': 'Zimbabwe'
      },
      {
        'code': 'ES',
        'name': 'Espagne'
      },
      {
        'code': 'LK',
        'name': 'Soudan du Sud'
      },
      {
        'code': 'SD',
        'name': 'Soudan'
      },
      {
        'code': 'SR',
        'name': 'République arabe sahraouie démocratique'
      },
      {
        'code': 'SJ',
        'name': 'Suriname'
      },
      {
        'code': 'SZ',
        'name': 'Svalbard et ile Jan Mayen'
      },
      {
        'code': 'SE',
        'name': 'Swaziland'
      },
      {
        'code': 'CH',
        'name': 'Suède'
      },
      {
        'code': 'SY',
        'name': 'Suisse'
      },
      {
        'code': 'TW',
        'name': 'Syrie'
      },
      {
        'code': 'TJ',
        'name': 'Tadjikistan'
      },
      {
        'code': 'TZ',
        'name': 'Thaïlande'
      },
      {
        'code': 'TH',
        'name': 'Togo'
      },
      {
        'code': 'TL',
        'name': 'Tokelau'
      },
      {
        'code': 'TG',
        'name': 'Tonga'
      },
      {
        'code': 'TK',
        'name': 'Trinité-et-Tobago'
      },
      {
        'code': 'TO',
        'name': 'Émirats arabes unis'
      },
      {
        'code': 'TT',
        'name': 'Tunisie'
      },
      {
        'code': 'TN',
        'name': 'Turquie'
      },
      {
        'code': 'TR',
        'name': 'Turkménistan'
      },
      {
        'code': 'TM',
        'name': 'Îles Turques-et-Caïques'
      },
      {
        'code': 'TC',
        'name': 'Tuvalu'
      },
      {
        'code': 'TV',
        'name': 'Ouganda'
      },
      {
        'code': 'UG',
        'name': 'Ukraine'
      },
      {
        'code': 'UA',
        'name': 'Macédoine (pays)'
      },
      {
        'code': 'AE',
        'name': 'Égypte'
      },
      {
        'code': 'GB',
        'name': 'Royaume-Uni'
      },
      {
        'code': 'US',
        'name': 'Guernesey'
      },
      {
        'code': 'UM',
        'name': 'Jersey'
      },
      {
        'code': 'UY',
        'name': 'Île de Man'
      },
      {
        'code': 'UZ',
        'name': 'Tanzanie'
      },
      {
        'code': 'VU',
        'name': 'États-Unis'
      },
      {
        'code': 'VE',
        'name': 'Îles Vierges des États-Unis'
      },
      {
        'code': 'VN',
        'name': 'Burkina Faso'
      },
      {
        'code': 'VG',
        'name': 'Uruguay'
      },
      {
        'code': 'VI',
        'name': 'Ouzbékistan'
      },
      {
        'code': 'WF',
        'name': 'Venezuela'
      },
      {
        'code': 'EH',
        'name': 'Wallis-et-Futuna'
      },
      {
        'code': 'YE',
        'name': 'Samoa'
      },
      {
        'code': 'ZM',
        'name': 'Yémen'
      },
      {
        'code': 'ZW',
        'name': 'Zambie'
      }
    ]
  }
});
