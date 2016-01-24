var app = angular.module('tweet-timeline', ['angular-loading-bar']);
app.controller('tweet-search', function($scope, $http, $sce) {
	$scope.loading = true;
	$scope.getTweets = function() {
		$http.get('/api/search.json?q='+$scope.search).success( function(response) {
			$scope.tweets = response.statuses;
			for (var i = 0; i < $scope.tweets.length; ++i) {
				$scope.tweets[i].text = $sce.trustAsHtml($scope.tweets[i].text);
				$scope.tweets[i].user.name = $sce.trustAsHtml($scope.tweets[i].user.name);
			}
			$scope.searchTerm = $scope.search
			$scope.loading = false;
		}, function(error) { 
			alert('Oh no! An error! '+error);
		});
	}
});