var app = angular.module('loklak', []);

app.controller("status", function($scope, $http) {
  $http.get('api/status.json').
    success(function(data, status, headers, config) {
      $scope.index_sizes = data.index_sizes;
    });
});