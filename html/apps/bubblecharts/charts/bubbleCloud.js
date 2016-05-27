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
      
    var diameter = 500,
    length = 100,
    color = d3.scale.linear().domain([1,length])
    .interpolate(d3.interpolateHcl)
    .range([d3.rgb("#FFFFFF"), d3.rgb('#17becf')]);


    var link = function ($scope, $el, $attrs) {
        
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
            .padding(5);
        
        // remember to set to true to watch values, if not $watch will not run as reference same
        $scope.$watch('data', update, true);
        
        
        // clip text while if zoom
        function clipText (d, t, scale) {
            if (d.r < 20/scale) {
                return "";
            }
            console.log(scale)
            var name = t.substring(0, d.r/scale);
            if (name.length < t.length) {
                name = name.substring (0, name.length - Math.min(4, name.length)) + "...";
            }
            return name;
        }
        
        // Setup zooming
        function zoomed() {
        var scale=d3.event.scale;
        var fontsize = 10;
        console.log('fontsize/scale')
        console.log(fontsize/scale)
          chart.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
          chart.selectAll('.nodeTextToClip')
          .style('font-size', function(d){ return d.r/scale/2.4})
          .text(function(d){return clipText(d, d.name,fontsize/scale/2.5 );})
        }

        var zoom = d3.behavior.zoom()
        .scaleExtent([-10, 50])
        .on("zoom", zoomed);
        zoom(svg);
        
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
            
            // input data
            var vis = chart.selectAll('.bubble')
            .data(nodes);
            // console.log(vis);
            
            // add attr to each entered group consisting of <circle> and <text>
            var enter = vis.enter().append('g')
            .attr('class', 'bubble')
            
            enter.append('circle')
            .attr('r', function(d) { return d.r; })
            .attr('class', function(d) { return d.className; })
            .attr('fill', function(d){                 
                return color(d.r+10); })
            
            // Add bubble text
            var text = enter.append("text")
            .attr("dy", ".1em")
            .style("text-anchor", "middle")
            
            text.append("tspan")
            .attr("class", "nodeTextToClip")
            .attr("x", "0")
            .attr("dy", "0")
            .style("font-size", function(d){ return d.r/2.5 ;})
            .text(function(d) { return clipText(d, d.name, 4); });
            
            text.append("tspan")
            .attr("x", "0")
            .attr("dy", function(d){return d.r/2})
            .style("font-weight", "100")
            .style("font-size", function(d){ return d.r/2.5 ;})
            .text(function(d) {
                return d.value;
            });
            
            // Transition the selected groups
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