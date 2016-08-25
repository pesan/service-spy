'use strict';

angular.module('tools.servicespy.config', ['api'])
.controller('ConfigController', function($scope, $state, ConfigService, config) {
    $scope.config = config;

    var swap = function(n, m, list) {
        var entry = list[n];
        list[n] = list[m];
        list[m] = entry;
    };

    $scope.moveUp = function(entry, list) {
        var index = _.indexOf(list, entry);
        swap(index, index - 1, list);
        $scope.saveConfig();
    };
    $scope.moveDown = function(entry, list) {
        var index = _.indexOf(list, entry);
        swap(index, index + 1, list);
        $scope.saveConfig();
    };
    $scope.editEntry = function(entry) {
        $scope.edit = entry;
    };
    $scope.saveConfig = function() {
        delete $scope.edit;
        ConfigService.save($scope.config).finally(function() {
            $state.reload();
        });
    };
    $scope.duplicate = function(entry, list) {
        list.splice(
            _.indexOf(list, entry),
            0,
            _.cloneDeep(entry)
        );
        $scope.saveConfig();
    };
    $scope.add = function(list) {
        var entry = {active: true};
        list.unshift(entry);
        $scope.editEntry(entry);
    };
    $scope.remove = function(entry, list) {
        _.pull(list, entry);
        $scope.saveConfig();
    };

    $scope.isEditing = function(entry) {
        return $scope.edit === entry;
    };

    $scope.isActive = function(entry) {
        return !!entry.active;
    };

    $scope.toggleActive = function(entry) {
        entry.active = !entry.active;
        $scope.saveConfig();
    };
})
.factory('ConfigService', function($api) {
    return {
        fetch: function() { return $api.get('/api/config'); },
        save: function(config) { return $api.put('/api/config', config); }
    };
});
