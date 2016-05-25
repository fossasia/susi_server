(function(){
    angular.module('myApp')
    
    .factory('dataService', 
    function($http){
        
        return {
            getTweets: getTweets
        }
        
        function getTweets(queryType, queryTerm){
            
            // user's tweets latest first
            var defaultUri = "/api/search.json?q=loklak";
            var uri = "";
            if(queryType === "search"){
                console.log(queryTerm);
                uri = "/api/search.json?q=" + queryTerm;
            } else {
                uri = defaultUri;
            }
            
            var promise = $http.get(uri)
            .then(searchCompleted)
            .catch(searchFailed);
            
            function searchCompleted(response){
                if (typeof response.data === 'object') {
                    console.log(response);
                    // return response.data.queries;                    
                    return response.data.statuses;
                } else {
                    // invalid response
                    console.log("Failed");
                }
            }
            
            function searchFailed(error) {
                console.log(error);
            }
            
            return promise;
        }
    }
)
}());