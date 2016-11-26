var app = angular.module('appListApp', ['loklak', 'ngTouch']);

app.controller("app_list", function($scope, $http) {
    $scope.apps = [];
    $scope.categoryKeys = [];
    $http.jsonp('/cms/apps.json?callback=JSON_CALLBACK')
    .success(function(data, status, headers, config) {
        $scope.categoryKeys = data.categories;
        $scope.apps = data.apps;
        $scope.categoryKeys.unshift('All');
    });

    $scope.categoryFilter = function(event) {
        item = event.target.id;
        if (item != 'All') {
            itemName = item.match(/[A-Z][a-z]+/g);
            $('#categoryName')[0].innerHTML = itemName.join(" ");
            $('div.span2').hide();
            qConstruct = 'div.span2#'+item;
            $(qConstruct).show();
            event.stopImmediatePropagation();
        }
        else {
            $('#categoryName')[0].innerHTML = 'Loklak Apps';
            $('div.span2').show();
        }
    }
});

app.filter('nospace', function () {
    return function (value) {
        return (!value) ? '' : value.replace(/ /g, '');
    };
});
