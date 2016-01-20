//javascript is moved here for simplicity and readability
var histogramApp = angular.module("histogramApp", ['ngResource']);

histogramApp.controller("LoklakCtrl", ["$scope", "$resource", function ($scope, $resource) {
    //in order to test locally, you need to add http://loklak.org before the uri
	    $scope.loklak = $resource('/api/:action',
	   {
            action:'search.json',
            q:'fossasia',
            callback:'JSON_CALLBACK',
            source:'cache',
            fields:'created_at'
       },
	   {
            get:{method:'JSONP'}
       });
                                
	    $scope.from = "2016-01-01";
	    $scope.to = "2016-01-20";
        $scope.query = "fossasia"
        //default
        
	  	$scope.doSearch = function () {
            var spinner = '<svg id="spinner" width="65px" height="65px" viewBox="0 0 66 66" xmlns="http://www.w3.org/2000/svg"><g><animateTransform attributeName="transform" type="rotate" values="0 33 33;270 33 33" begin="0s" dur="1.4s" fill="freeze" repeatCount="indefinite"/><circle fill="none" stroke-width="6" stroke-linecap="round" cx="33" cy="33" r="30" stroke-dasharray="187" stroke-dashoffset="610"><animate attributeName="stroke" values="#4285F4;#DE3E35;#F7C223;#1B9A59;#4285F4" begin="0s" dur="5.6s" fill="freeze" repeatCount="indefinite"/><animateTransform attributeName="transform" type="rotate" values="0 33 33;135 33 33;450 33 33" begin="0s" dur="1.4s" fill="freeze" repeatCount="indefinite"/><animate attributeName="stroke-dashoffset" values="187;46.75;187" begin="0s" dur="1.4s" fill="freeze" repeatCount="indefinite"/></circle></g></svg>';
            //move to inner scope for clarity
	  		var date1 = new Date($scope.from);
			var date2 = new Date($scope.to);
			var timeDiff = Math.abs(date2.getTime() - date1.getTime());
			var diffDays = Math.ceil(timeDiff / (1000 * 3600 * 24)); 
			if (diffDays < 5){
				alert('Kindly provide a difference of more than 4 days between the two dates.')
			} else
            {
				if ($scope.query === ""){
					alert("Kindly provide a query to search for!");
				} else
                {
			        $("#bar-example")[0].innerHTML = spinner;
			      	$scope.loklakResult = $scope.loklak.get
                    ({q:$scope.query+' since:'+$scope.from+' untill:'+$scope.to},function(){
			        		$("#bar-example")[0].innerHTML = "";
					        var data_list = [];
					      	angular.forEach($scope.loklakResult.aggregations.created_at, function(i,item){
					      		data_list.push({
					      			"y": item,
					      			"a": i
					      		})
					      	});
                        //draw the bar
					      	Morris.Bar({
					      	  element: 'bar-example',
					      	  data: data_list,
					      	  xkey: 'y',
					      	  ykeys: ['a'],
					      	  labels: ['Total Tweets']
					      	});
				        }
				    ); //end of $scope.loklak.get func
		        }
		    }
        };
	}]);