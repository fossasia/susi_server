(function(){
    angular.module('myApp')
    
    .factory('queryService', 
    function(dataService, analyticService, $localStorage){
        
        return {
            queryTweets: queryTweets
        };
        
        function queryTweets(queryType, queryTerm, dateFrom, dateTo, maxHashtags){
            dataService.getTweets(queryType, queryTerm, dateFrom, dateTo).then(function(data){
                
                var newTweets;
                if(!$localStorage.tweets || $localStorage.tweets.length === 0){
                    newTweets = data;
                    $localStorage.tweets = data;
                } else {
                    newTweets = analyticService.getNewTweets(data);
                    $localStorage.tweets = newTweets.concat($localStorage.tweets);
                }           
                
                // process any new tweets
                if(!newTweets.length) {
                    console.log("no new tweets");
                } else {
                   // analyticService.updateWordFreq(newTweets);
                   // analyticService.updateMentionFreq(newTweets);

                   // process all for hashtagfreq
                   analyticService.updateHashtagDateFreq(data, maxHashtags);
                }
                
            });
        }
        
    })
}());