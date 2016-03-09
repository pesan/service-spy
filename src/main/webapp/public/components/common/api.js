'use strict';

angular.module('api', [])
.factory('$api', function($http, $q) {
    var sourceByUri = {};
    var unwrapData = function(result) { return result.data; };
    return {
        get: function(uri, config) {
            return $http.get(uri, config).then(unwrapData);
        },
        post: function(uri, data, config) {
            return $http.post(uri, data, config).then(unwrapData);
        },
        put: function(uri, data, config) {
            return $http.put(uri, data, config).then(unwrapData);
        },
        delete: function(uri, config) {
            return $http.delete(uri, config).then(unwrapData);
        },
        stream: function(uri) {
            if (sourceByUri[uri]) {
                sourceByUri[uri].source.close();
                sourceByUri[uri].deferred.resolve();
            }
            var deferred = $q.defer();
            var source = new EventSource(uri);
            source.onmessage = function(event) {
                deferred.notify(JSON.parse(event.data));
            };
            source.onerror = function(error) {
                deferred.reject(error);
            };
            sourceByUri[uri] = {source: source, deferred: deferred};
            return deferred.promise;
        },
        canStream: function() {
            return typeof(EventSource) !== 'undefined';
        }
    }
});
