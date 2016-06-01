'use strict';

angular.module('tools.servicespy', [
    'ngAnimate',
    'ngRoute',
    'ui.router',
    'tools.servicespy.action',
    'tools.servicespy.config'
])
.config(function ($urlRouterProvider, $locationProvider, $stateProvider) {
    $locationProvider.html5Mode(false);
    $urlRouterProvider.otherwise('/');
    $stateProvider
        .state('config', {
            url: '/config/',
            templateUrl: '/components/config/view.html',
            controller: 'ConfigController',
            resolve: {
                config: function(ConfigService) {
                    return ConfigService.fetch();
                }
            }
        })
        .state('actions', {
            url: '/?filter&show&stream',
            templateUrl: '/components/action/view.html',
            controller: 'ActionController',
            resolve: {
                filter: function($stateParams) {
                    return decodeURIComponent($stateParams.filter || '');
                },
                show: function($stateParams) {
                    return $stateParams.show === 'true';
                },
                canStream: function($api) {
                    return $api.canStream();
                },
                stream: function($stateParams, canStream) {
                    return canStream && $stateParams.stream === 'true';
                },
                entryState: function(ActionService, show, $timeout, textFormatter, stream, Toast) {
                    var state = {
                        active: stream,
                        entries: []
                    };
                    var clearEntries = function() {
                        while (state.entries.length) {
                            state.entries.pop();
                        }
                    };
                    var handleEntry = function(entry) {
                        if (_.some(state.entries, 'id', entry.id)) {
                            clearEntries();
                        }
                        var formatter = textFormatter(entry.response.contentType);
                        entry.expanded = show;
                        entry.requestData = formatter(entry.request.data);
                        entry.responseData = formatter(entry.response.data);
                        entry.isNew = stream;
                        state.entries.unshift(entry);
                        $timeout(function() { delete entry.isNew; }, 3000);
                    };
                    if (stream) {
                        ActionService.listen(function() {
                            Toast('warning', 'Connection with server lost. Retrying...');
                            clearEntries();
                        }).then(function(){}, function(){}, handleEntry);
                    } else {
                        ActionService.fetch().then(function(entries) {
                            _.forEach(entries, handleEntry);
                        });
                    }

                    return state;
                }
            }
        });
}).
config(function($httpProvider) {
    $httpProvider.interceptors.push(function($q, Toast) {
        return {
            responseError: function(err) {
                Toast('danger', 'Communication error (' + err.status + ')' + (err.statusText ? ': ' + err.statusText : ''));
                return $q.reject(err);
            }
        }
    });
})
.directive('toasts', function($rootScope) {
    return {
        restrict: 'E',
        link: function(scope) {
            $rootScope.$watch('toasts', function(toasts) {
                scope.toasts = toasts;
            }, true);
        },
        template:
            '<div class="toasts">' +
            '   <div ng-repeat="toast in toasts" class="toast alert alert-{{ toast.type }}" title="{{ toast.time | date: \'yyyy-MM-dd HH:mm:ss\'}}" role="alert">' +
            '       {{ toast.message }}' +
            '   </div>' +
            '</div>'
    };
})
.factory('Toast', function($rootScope, $timeout) {
    return function(type, message) {
        var toast = { type: type, message: message, time: new Date() };
        ($rootScope.toasts = $rootScope.toasts || []).unshift(toast);
        $timeout(function() {
            $rootScope.toasts = _.without($rootScope.toasts, toast);
        }, 5000);
    };
})
.factory('textFormatter', function($filter) {
    var formatters = {
        'application/xml': $filter('xml'),
        'application/json': function(text) { return _.isEmpty(text) ? "" : angular.toJson(angular.fromJson(text), 2); }
    };
    return function(contentType) {
        return formatters[(contentType || '').replace(/;.*/, '')] || _.identity;
    };
});
