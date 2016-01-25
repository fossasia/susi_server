var app = angular.module('histogramApp', []);

app.controller("histogram", function($scope, $http) {
  $http.jsonp('http://loklak.org/api/search.json?callback=JSON_CALLBACK&q=fossasia%20since:2015-12-10&source=cache&count=0&fields=created_at')
       .then(function(response) {
         points = [];
         for (var key in response.data.aggregations.created_at) {
           points.push({ day: key, value: response.data.aggregations.created_at[key] });
         }

         console.log(points);

         new Morris.Line({
           // ID of the element in which to draw the chart.
           element: 'myfirstchart',
           // Chart data records -- each entry in this array corresponds to a point on
           // the chart.
           data: points,
           // The name of the data record attribute that contains x-values.
           xkey: 'day',
           // A list of names of data record attributes that contain y-values.
           ykeys: ['value'],
           // Labels for the ykeys -- will be displayed when you hover over the
           // chart.
           labels: ['Value']
         });
       });

});
