'use strict';

angular.module('tools.servicespy.action', ['api'])
.controller('ActionController', function($scope, $state, ActionService, entryState, filter, canStream, contentViews) {
    $scope.state = entryState;
    $scope.requestLogs = entryState.entries;
    $scope.filter = filter;
    $scope.canStream = canStream;
    $scope.contentViews = contentViews;

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
        return entry.response.status < 300;
    };

    $scope.isRedirectStatus = function(entry) {
        return entry.response.status >= 300 && entry.response.status < 400
    };

    $scope.isErrorStatus = function(entry) {
        return entry.response.status >= 400;
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
.factory('contentViews', function() {
    return [
        {
            name: 'Image',
            pattern: /image\/.*/,
            template: '<img class="dataview" ng-src="{{ model.href }}">'
        },
        {
            name: 'XML',
            pattern: /application\/.*xml.*/,
            template: '<pre class="dataview">{{ model.data | pretty:\'xml\' }}</pre>'
        },
        {
            name: 'JSON',
            pattern: /application\/json/,
            template: '<pre class="dataview">{{ model.data | pretty:\'json\'}}</pre>'
        },
        {
            name: 'CSS',
            pattern: /text\/css.*/,
            template: '<pre class="dataview">{{ model.data | pretty:\'css\' }}</pre>'
        },
        {
            name: 'Plain text',
            pattern: /application\/javascript.*|text\/.*/,
            template: '<pre class="dataview">{{ model.data }}</pre>'
        },
        {
            name: 'Binary',
            pattern: /.*/,
            template: '<pre class="dataview">{{ model.data | hex }}</pre>'
        }
    ];
})
.directive('dataview', function($compile, $timeout, contentViews) {
    return {
        restrict: 'E',
        scope: {
            model: '='
        },
        link: function(scope, element) {
            scope.$watch('model.view', function(next, current) {
                element.empty();
                element.append($compile(scope.model.view.template)(scope));
            });
        }
    };
})
.directive('activateTab', function() {
    return {
        restrict: 'A',
        scope: {
            activateTab: '@'
        },
        link: function(scope, element) {
            element.attr('role', 'tab');
            element.attr('data-toggle', 'tab');
            element.attr('href', '#' + scope.activateTab);
            element.on('click', function(event) {
                event.preventDefault();
            });
        }
    }
})
.directive('httpentry', function() {
    return {
        restrict: 'E',
        scope: {
            id: '@',
            name: '@',
            contentViews: '=',
            entry: '=',
            model: '='
        },
        template:
            '<div>' +
            '   <div class="card-header">' +
            '      <ul class="nav nav-tabs card-header-tabs pull-xs-left" role="tablist">' +
            '           <li class="nav-item" ng-if="model.exception">' +
            '               <a class="nav-link" ng-class="{active: model.exception}" activate-tab="{{ id }}-exception-{{ entry.id }}">Exception</a>' +
            '           </li>' +
            '           <li class="nav-item">' +
            '               <a class="nav-link" ng-class="{active: !model.exception}" activate-tab="{{ id }}-data-{{ entry.id }}">{{ name }} Data</a>' +
            '           </li>' +
            '           <li class="nav-item" ng-if="model.headers">' +
            '               <a class="nav-link" activate-tab="{{ id }}-headers-{{ entry.id }}">Headers</a>' +
            '           </li>' +
            '       </ul>' +
            '       <div class="pull-xs-right">' +
            '           <a class="btn btn-sm btn-primary" ng-href="{{ model.href }}"><i class="fa fa-download"></i></a>' +
            '       </div>' +
            '   </div>' +
            '   <div class="card-block tab-content">' +
            '       <div class="tab-pane dataview" ng-class="{active: model.exception}" id="{{ id }}-exception-{{ entry.id }}" role="tabpanel">' +
            '           <exception class="dataview" value="model.exception"></exception>' +
            '       </div>' +
            '       <div class="tab-pane" ng-class="{active: !model.exception}" id="{{ id }}-data-{{ entry.id }}" role="tabpanel">' +
            '           <div class="dropdown open">' +
            '               <button class="btn btn-sm btn-secondary dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">' +
            '                   {{ model.view.name }}' +
            '               </button>' +
            '               <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">' +
            '                   <button ng-repeat="view in contentViews" ng-click="model.view = view" class="dropdown-item">{{ view.name }}</a>' +
            '               </div>' +
            '           </div>' +
            '           <hr/>' +
            '           <dataview model="model"></dataview>' +
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
.filter('pretty', function() {
    return function(text, type, level) {
        try {
            return _.isEmpty(text) ? '' : vkbeautify[type](text, level || 2);
        } catch (e) {
            return text;
        }
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
