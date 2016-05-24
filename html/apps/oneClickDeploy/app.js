var app = angular.module("PeersCount", []);
app.controller("getPeersCount", ["$scope", "$http", "$sce", function($scope, $http, $sce) {
    $http.jsonp(String("http://loklak.org/api/peers.json")).success(function(response) {
        $scope.data = response.count;
    });
}]);