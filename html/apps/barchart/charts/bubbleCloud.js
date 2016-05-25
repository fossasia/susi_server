(function(){
angular.module('myApp')

.controller('bubbleCloudCtrl', ['$scope','$window','$localStorage',

  function ($scope, $window, $localStorage) {
      
      $scope.$storage = $localStorage;
    //   $window.addEventListener('resize', function () {
    //       $scope.$broadcast('windowResize');
    //   });
    
  }
])


.directive('bubbleCloud', [ 

  function () {

    var link = function ($scope, $el, $attrs) {
        
        var diameter = 500;

        var id = '#bubbleCloud_'+$scope.$id;

        
        var svg = d3.select($el[0]).append('svg')
            .attr('width', diameter)
            .attr('height', diameter);
            
        var chart = svg.append("g");
        // chart.append("text").attr("id", "loading")
        //     .text("Loading...")
        //     .attr("transform", "translate(200,250)");
            
        var bubble = d3.layout.pack()
            .size([diameter, diameter])
            .value(function(d) {return d.size;}) // more mentions, bigger bubble
            .sort(function(a, b) { // biggest in the middle
                return -(a.value - b.value);
            }) 
            .padding(3);
        
        // remember to set to true to watch values, if not $watch will not run as reference same
        $scope.$watch('data', update, true);
        
        function update(mentionFreq) {
            // console.log("updating");
            // console.log(mentionFreq);
            // if new poll, don't enter, fade out everything
            if(!mentionFreq || Object.keys(mentionFreq).length === 0) {
                
                chart.selectAll("text")
                .transition().duration(1000)
                .attr("opacity", 0)
                
                chart.selectAll("circle")
                .transition().duration(1000)
                .attr("r", 0)
                
                window.setTimeout(function(){
                    // select all bubble groups, remove data then remove dom nodes
                    chart.selectAll('.bubble').data([])
                    .exit().remove();
                },1001);
                
                return;
            }
            // if ($scope.mentionFreq) chart.select("#loading").remove();
            
            
            function processData(obj) {
                var newDataSet = [];
                for(var prop in obj) {
                    if(obj[prop] >= $scope.min){
                        var node = {name: prop, className: 'bubbles', size: obj[prop]}
                        newDataSet.push(node);
                    }  
                }
                return {children: newDataSet};
            }     
            
            // generate data with calculated layout values
            var nodes = bubble.nodes(processData(mentionFreq))
                .filter(function(d) { return !d.children; }); 
            // filter out the outer bubble for pack layout
            // console.log(nodes);
            
            var vis = chart.selectAll('.bubble')
            .data(nodes);
            
            // console.log(vis);
            
            // add attr to each entered group consisting of <circle> and <text>
            var enter = vis.enter().append('g')
            .attr('class', 'bubble')
            
            enter.append('circle')
            .attr('r', function(d) { return d.r; })
            .attr('class', function(d) { return d.className; })
            
            enter.append("text")
            .attr("dy", ".1em")
            .style("text-anchor", "middle")
            .text(function (d) { return d.name; });

            // transition the selected groups
            vis.transition().duration(1000)
            .attr("transform", function (d) { 
                return "translate(" + d.x + "," + d.y + ")";
            });
                        
            // resize();
        };

        // function resize() {
        //     svg.attr("width", $el[0].clientWidth);
        //     svg.attr("height", $el[0].clientWidth); 
        // }
        
        // $scope.$on('windowResize',resize);

    };
    return {
      template:'<div layout="column"><h2 style="text-align:center">{{title}}</h2><div id="bubbleCloud_{{$id}}"</div></div>',
      link: link, 
      restrict: 'E',
      scope:{
          // 2 way bind the data, literals for min & title
          data: '=',
          min: '@',
          title: '@'
      }
    };
}])
        
}());