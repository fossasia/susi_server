var app = angular.module('appListApp', []);

app.controller("app_list", function($scope, $http) {
  $scope.apps = [];
  $http.jsonp('/api/apps.json?callback=JSON_CALLBACK')
    .success(function(data, status, headers, config) {
      for (var i = 0; i < data.apps.length; i++) {
        $scope.apps.push(data.apps[i]);
      }
    });
});
