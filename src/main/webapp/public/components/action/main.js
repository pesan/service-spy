'use strict';

angular.module('tools.servicespy.action', ['api'])
.controller('ActionController', function($scope, $state, ActionService, entryState, filter, canStream) {
    $scope.state = entryState;
    $scope.requestLogs = entryState.entries;
    $scope.filter = filter;
    $scope.canStream = canStream;

    $scope.deleteAll = function() {
        while (entryState.entries.length) {
            entryState.entries.pop();
        }
    };

    $scope.refresh = function(params) {
        var allParams = _.assign({}, params);
        allParams.filter = $scope.filter;
        $state.go('actions', allParams, {reload: true});
    };

    $scope.toggle = function(entry) {
        entry.expanded = !entry.expanded;
    };

    $scope.toggleAll = function(show) {
        $scope.refresh({ show: show });
    };
})
.factory('ActionService', function($api) {
    return {
        fetch: function() { return $api.get('/api/actions'); },
        listen: function(onClose) { return $api.stream('/api/actions/stream', onClose); }
    };
})
.directive('icon', function() {
    return {
        restrict: 'E',
        scope: {
            name: '@'
        },
        template: _.constant('<i class="fa fa-{{name}}"></i>')
    };
})
.directive('exception', function() {
    return {
        restrict: 'E',
        scope: {
            value: '='
        },
        template: _.constant(
            '<code>Exception: {{ value.message }}</code>' +
            '<ul class="list-unstyled" style="margin-left: 20px;">' +
            '   <li ng-repeat="entry in value.stackTrace">' +
            '        <code>at&nbsp;{{ entry.className }}.{{ entry.methodName }}({{entry.fileName}}:<span ng-if="!entry.nativeMethod">{{ entry.lineNumber }}</span><span ng-if="entry.nativeMethod">&lt;native&gt;</span>)</code>' +
            '   </li>' +
            '</ul>'
        )
    };
})
.filter('xml', function() {
    return function(text, pattern) {
        return text && vkbeautify.xml(text, pattern);
    };
});