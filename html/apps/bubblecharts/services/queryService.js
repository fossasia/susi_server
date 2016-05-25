(function(){
    angular.module('myApp')
    
    .factory('queryService', 
    function(dataService, analyticService, $localStorage){
        
        return {
            queryTweets: queryTweets
        };
        
        function queryTweets(queryType, queryTerm){
            dataService.getTweets(queryType, queryTerm).then(function(data){
                
                var newTweets;
                if(!$localStorage.tweets || $localStorage.tweets.length === 0){
                    newTweets = data;
                    $localStorage.tweets = data;
                } else {
                    newTweets = analyticService.getNewTweets(data);
                    $localStorage.tweets = newTweets.concat($localStorage.tweets);
                }            
                analyticService.updateWordFreq(newTweets);
                analyticService.updateMentionFreq(newTweets);
                analyticService.updateHashtagFreq(newTweets);
                
            });
        }
        
    })
}());