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
                entryState: function(ActionService, show, $timeout, stream, Toast) {
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
                        entry.expanded = show;
                        entry.request.data = atob(entry.request.data);
                        entry.response.data = atob(entry.response.data);
                        entry.request.href = entry.href.requestData;
                        entry.response.href = entry.href.responseData;
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
});
