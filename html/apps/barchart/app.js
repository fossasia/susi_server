angular.module('myApp', ['ngStorage', 'ngMaterial', 'ngMessages' ,'ngSanitize', 'angular-momentjs', 'loklak'])

// dataService.polltweets(@querytype, @queryterm) -> polls & adds to $localStorage
// $localStorage.tweets: stores tweet objs in array

// eg. api call ("/api/search.json?q=from:myhandle")
// querytype: eg. "search" (using /api/search.json)
// queryterm: eg. "from:myhandle" (refer to http://loklak.org/api.html for more)


.controller('MainCtrl', ['$scope', '$interval', '$localStorage', '$q','$sce', 'queryService',
    function($scope, $interval, $localStorage, $q, $sce, queryService) {
        
        var vm = this;
        // store promise so can cancel on next query
        var promise;
        
        $scope.apis = ('search suggest').split(' ').map(function(apitype) {
            return {type: apitype}
        })
        
        $scope.maxDate = new Date();
        $scope.query = {
            type: 'search',
            term: 'loklak',
            interval: 1,
            maxHashtags: 3
        }
        
        $scope.$storage = $localStorage.$default({
          tweets: [],
          wordFreq: {},
          mentionFreq: {},
          hashtagDateFreq: [],
        })
        // $scope.$storage.tweets=[];
        window.$scope = $scope;
        
        $scope.handlify = function(handle){
            return $sce.trustAsHtml("<a href=\"https://twitter.com/" + handle + "\">@" + handle + "</a>");
        }
        $scope.rmvSpan = function(username) {
            return username.replace(/<img.*?false" alt="(.).*?>/g,"$1").replace(/&nbsp;/g," ");
        };
        $scope.poll = function(type, term, interval, dateFrom, dateTo, maxHashtags){
            
            // clear previous storage & display & interval
            $scope.$storage.tweets = [];
            $scope.$storage.wordFreq = {};
            $scope.$storage.mentionFreq = {};
            $scope.$storage.hashtagDateFreq = [];

            $interval.cancel(promise);
            vm.wallTweets = [];
            vm.limit = 25
            
            // clear form
            // $scope.query={};
            
            // first call
            queryService.queryTweets(type, term, dateFrom, dateTo, maxHashtags);

            // poll every few minutes
            promise = $interval(function(){
                queryService.queryTweets(type, term, dateFrom, dateTo, maxHashtags);
            } , interval*60000);
        }
        
        $scope.tweetify = function(_str){
            if (!_str) {
                return;
            }
            var _text = _str.replace(
                /https?:\/\/[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]/gi,
                function($0) {
                    var innerhtml = $0;
                    if($0.length > 50){
                        innerhtml = $0.substring(0,50) + '...';
                    } 
                    return '<a href=\"' + $0 + '\">' + innerhtml + '</a>';
                });

            // bugfix
            if (!_text) {
                return '';
            }
            
            // Twitter
            _text = _text.replace(/(|\s)*@([\u00C0-\u1FFF\w]+)/g, 
                '$1<a href="https://twitter.com/$2" target="_blank">@$2</a>');
            _text = _text.replace(/(^|\s)*#([\u00C0-\u1FFF\w]+)/g, 
                '$1<a href="https://twitter.com/search?q=%23$2" target="_blank">#$2</a>');
            return _text;
            
        }
    }
])
.config(function($mdThemingProvider) {
  // Configure a dark theme with primary foreground yellow
  $mdThemingProvider.theme('docs-dark', 'default')
    .primaryPalette('yellow')
    .dark();
});
