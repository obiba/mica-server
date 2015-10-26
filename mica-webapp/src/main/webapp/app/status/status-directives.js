'use strict';

mica.status
  .directive('statusButtons', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        state: '=',
        onEdit: '&',
        onPublish: '&',
        onUnPublish: '&',
        onDelete: '&',
        toDraft: '&',
        toUnderReview: '&',
        toDeleted: '&'
      },
      templateUrl: 'app/status/status-buttons-template.html'
    };
  }])

  .directive('fileStatusButtons', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        document: '=',
        onEdit: '&',
        canEdit: '&',
        onPublish: '&',
        onUnPublish: '&',
        canPublish: '&',
        onDelete: '&',
        canDelete: '&',
        toDraft: '&',
        toUnderReview: '&',
        toDeleted: '&'
      },
      templateUrl: 'app/status/file-status-buttons-template.html'
    };
  }]);
