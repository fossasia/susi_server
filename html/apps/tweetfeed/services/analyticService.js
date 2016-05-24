(function(){
    angular.module('myApp')
    
    .factory('analyticService', 
    function($localStorage){
        
        return {
            getNewTweets: getNewTweets
        }
        
        // returns an array of new tweets, comparing with those in local storage
        function getNewTweets(statuses){
            // initial oldest date from first element of last poll
            var prevNewest = new Date($localStorage.tweets[0].created_at);
            var idx =0;
            var tweetDate= new Date(statuses[idx].created_at);
            var currOldest = new Date(statuses[statuses.length-1].created_at);
            
            var newTweets =[];
            // if the whole array is new prepend to localStorage tweets array
            if(prevNewest < currOldest){
                $localStorage.tweets = statuses.concat($localStorage.tweets);
                newTweets = statuses;
            } else {
                // else prepend only new tweets to localStorage tweets array in desc order
                while(tweetDate > prevNewest && idx !== statuses.length){
                    newTweets.push(statuses[idx]);
                    console.log($localStorage.tweets);
                    idx++;
                    tweetDate= new Date(statuses[idx].created_at);
                }
            }
            return newTweets;
        }
    }
)

}());