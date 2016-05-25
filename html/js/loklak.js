var app = angular.module("loklak", ["ngRoute"]);
app.controller("status", function($scope, $http) {
  $http.get("api/status.json").
    success(function(data, status, headers, config) {
      $scope.index = data.index;
    });
});

app.controller("search", function($scope, $http) {
  $scope.query = '';
  $scope.results = [];
  $scope.search = function() {
    if ($scope.query != '') {
      $scope.results = [];
      $http.get("/api/search.json?q=" + $scope.query).
        success(function(data, status, headers, config) {
          for (var i = 0; i < data.statuses.length; i++) {
            $scope.results.push(data.statuses[i].text);
          }
        });
    }
  }
});

app.controller("topmenu", function($scope, $http) {
  $http.get("/cms/topmenu.json").
    success(function(data, status, headers, config){
        $scope.results = data.items;
    });
});

app.filter("reverse", function() {
  return function(items) {
    if (!items || !items.length) {
      return;
    }
    return items.slice().reverse();
  };
});

angular.module("loklak")
.config(function ($routeProvider) {
    $routeProvider.
    when("/index", {
        controller: "widgetsController",
        activetab: "Home"
    }).
    when("/about", {
        controller: "widgetsController",
        activetab: "About"
    }).
    when("/showcase", {
        controller: "widgetsController",
        activetab: "Showcase"
    }).
    when("/architecture", {
        controller: "widgetsController",
        activetab: "Architecture"

    }).
    when("/download", {
        controller: "widgetsController",
        activetab: "Download"
    }).
    when("/tutorials", {
        controller: "widgetsController",
        activetab: "Tutorials"

    }).
    when("/api", {
        controller: "widgetsController",
        activetab: "API"
    }).
    when("/dump", {
        controller: "widgetsController",
        activetab: "Dumps"

    }).
    when("/apps", {
        controller: "widgetsController",
        activetab: "Apps"
    });
});

function widgetsController($scope, $route) {
    $scope.$route = $route;
}
