const app = angular.module("loklak", ['ngRoute']);
app.controller("status", function($scope, $http) {
  $http.get("/aggregation/status.json").
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
      $http.get("/aaa/search.json?q=" + $scope.query).
        success(function(data, status, headers, config) {
          for (let i = 0; i < data.statuses.length; i++) {
            $scope.results.push(data.statuses[i].text);
          }
        });
    }
  }
});

app.filter("reverse", function() {
  return function(items) {
    if (!items || !items.length) {
      return;
    }
    return items.slice().reverse();
  };
});

angular.element(document).ready(function () {
  let navString = "";
  const winLocation = window.location.href;
  $.getJSON("/cms/topmenu.json", function(data) {
    navItems = data.items;
    navItems = navItems.reverse();
    let count = 0;
    $.each( navItems, function(index, itemData) {
      name = Object.keys(itemData);
      link = itemData[name];
      // Now construct the li items
      liItem = "<li>";
      if (winLocation.indexOf(link) != -1 && count != 1) {
        liItem = "<li class='active'>";
        count = count + 1;
      }
      if(name == "Blog") { // The Blog tab redirects to the loklak blog (http://blog.loklak.net/)
        liItem += "<a href='"+link+"'>"+name+"</a></li>";
      }
      else {
        liItem += "<a href='\/"+link+"'>"+name+"</a></li>";
      }
      liItem = $(liItem);
      $('#navbar').find('> ul').prepend(liItem);
    });
  });
});
