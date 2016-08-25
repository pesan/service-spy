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

    $scope.isOkStatus = function(entry) {
        return parseInt(entry.response.status/100) <= 2;
    };

    $scope.isRedirectStatus = function(entry) {
        return parseInt(entry.response.status/100) === 3;
    };

    $scope.isErrorStatus = function(entry) {
        return parseInt(entry.response.status/100) >= 4;
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
        template: '' +
            '<code>Exception: {{ value.message }}</code>' +
            '<ul class="list-unstyled" style="margin-left: 20px;">' +
            '   <li ng-repeat="entry in value.stackTrace">' +
            '        <code>at&nbsp;{{ entry.className }}.{{ entry.methodName }}({{entry.fileName}}:<span ng-if="!entry.nativeMethod">{{ entry.lineNumber }}</span><span ng-if="entry.nativeMethod">&lt;native&gt;</span>)</code>' +
            '   </li>' +
            '</ul>'
    };
})
.directive('dataview', function($compile) {
    var contentViews = [
        { pattern: /image\/.*/, template: '<img ng-src="{{ model.href }}">' },
        { pattern: /application\/.*xml.*/, template: '<pre>{{ model.data | xml }}</pre>' },
        { pattern: /application\/json/, template: '<pre>{{ model.data | json }}</pre>' },
        { pattern: /text\/.*/, template: '<pre>{{ model.data }}</pre>' },
        { pattern: /.*/, template: '<pre>{{ model.data | hex }}</pre>' }
    ];
    return {
        restrict: 'E',
        scope: {
            model: '='
        },
        link: function(scope, element) {
            var contentType = (scope.model.contentType || '').replace(/;.*/, '');
            var view = _.find(contentViews, function(view) {
                return view.pattern.test(contentType);
            });
            element.replaceWith($compile(view.template)(scope));
        }
    };
})
.directive('httpentry', function() {
    return {
        restrict: 'E',
        scope: {
            id: '@',
            name: '@',
            entry: '=',
            model: '='
        },
        template:
            '<div>' +
            '   <div class="card-header">' +
            '      <ul class="nav nav-tabs card-header-tabs pull-xs-left" role="tablist">' +
            '           <li class="nav-item">' +
            '               <a class="nav-link active" ng-href="#{{ id }}-data-{{ entry.id }}" role="tab" data-toggle="tab">{{ name }} Data</a>' +
            '           </li>' +
            '           <li class="nav-item">' +
            '               <a class="nav-link" ng-href="#{{ id }}-headers-{{ entry.id }}" role="tab" data-toggle="tab">Headers</a>' +
            '           </li>' +
            '       </ul>' +
            '       <div class="pull-xs-right">' +
            '           <a class="btn btn-sm btn-primary" ng-href="{{ model.href }}"><i class="fa fa-download"></i></a>' +
            '       </div>' +
            '   </div>' +
            '   <div class="card-block tab-content">' +
            '       <div class="tab-pane active" id="{{ id }}-data-{{ entry.id }}" role="tabpanel">' +
            '           <dataview ng-if="!model.exception" model="model"></dataview>' +
            '           <exception ng-if="model.exception" value="model.exception"></exception>' +
            '       </div>' +
            '       <div class="tab-pane" id="{{ id }}-headers-{{ entry.id }}" role="tabpanel">' +
            '           <table class="table table-sm">' +
            '               <tbody>' +
            '                   <tr ng-repeat="(key, values) in model.headers">' +
            '                       <th>{{ key }}</th>' +
            '                       <td><code ng-repeat="value in values">{{ value }}</code></td>' +
            '                   </tr>' +
            '               </tbody>' +
            '           </table>' +
            '       </div>' +
            '   </div>' +
            '</div>'
    };
})
.filter('xml', function() {
    return function(text, level) {
        return _.isEmpty(text) ? '' : vkbeautify.xml(text, level || 2);
    };
})
.filter('json', function() {
    return function(text, level) {
        return _.isEmpty(text) ? '' : angular.toJson(angular.fromJson(text), level || 2);
    };
})
.filter('hex', function() {
    return function(text) {
        var result = '';
        for (var i = 0; i < text.length; i += 8) {
            var hexColumn = '', charColumn = '';
            for (var c = i; c < i + 8; c++) {
                if (c < text.length) {
                    var ch = text.charCodeAt(c);
                    hexColumn += _.padLeft(ch.toString(16), 2, '0') + ' ';
                    charColumn += ch >= 32 ? text.charAt(c) : '.'
                } else {
                    hexColumn += "   ";
                    charColumn += " ";
                }
            }
            result += _.padLeft(i.toString(16), 8, '0') + '  ' + hexColumn + ' ' + charColumn + '\n';
        }
        return result;
    };
});
