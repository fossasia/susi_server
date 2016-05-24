(function(){
    angular.module('myApp')
    
    .factory('dataService', 
    function($http, $moment){
        
        return {
            getTweets: getTweets
        }
        
        function getTweets(queryType, queryTerm, dateFrom, dateTo){
            
            // user's tweets latest first
            var defaultUri = "/api/search.json?q=loklak";
            var uri = "";
            if(queryType === "search"){
                console.log(queryTerm);
                if(typeof(dateFrom) !== "undefined" || typeof(dateTo) !== "undefined"){
                    var dateFromF = $moment(dateFrom).format('YYYY-MM-DD') || "";
                    var dateToF = $moment(dateTo).format('YYYY-MM-DD') || "";
                    uri = "/api/search.json?q=" + queryTerm + "+since:" + dateFromF + "+until:" + dateToF;
                    console.log(uri);
                    
                } else {
                    uri = "/api/search.json?q=" + queryTerm;
                }
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