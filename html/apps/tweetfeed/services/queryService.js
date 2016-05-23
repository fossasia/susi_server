(function(){
    angular.module('myApp')
    
    .factory('queryService', 
    function(dataService, analyticService, $localStorage){
        
        return {
            queryTweets: queryTweets
        };
        
        function queryTweets(queryType, queryTerm){
            dataService.getTweets(queryType, queryTerm).then(function(data){
                // get newest tweet date from local storage
                if($localStorage.tweets.length === 0){
                    $localStorage.tweets = data;                    
                } else {
                    var newTweets = analyticService.getNewTweets(data);
                    $localStorage.tweets = newTweets.concat($localStorage.tweets);
                }
            });
        }
    })
}());