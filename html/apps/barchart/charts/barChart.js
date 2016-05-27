(function(){
    
    angular.module('myApp')
    
    .controller('barChartCtrl', ['$scope','$window','$localStorage',
    
    function ($scope, $window, $localStorage) {
        $scope.$storage = $localStorage;        
    }
])

.directive('barChart', [ 
    function () {
        
        var link = function ($scope, $el, $attrs) {
            
            // svg setup
            var margin = {top: 20, right: 50, bottom: 60, left: 0},
            width = 600 - margin.left - margin.right,
            height = 500 - margin.top - margin.bottom,
            padding = 100;
            
            var x = d3.scale.ordinal().rangeRoundBands([0, width], .3);
            var y = d3.scale.linear().rangeRound([height, 0]);
            var color = d3.scale.category20();
            var xAxis = d3.svg.axis().scale(x).orient("bottom");
            // whole number y axis ticks
            var yAxis = d3.svg.axis().scale(y).orient("right").tickFormat(d3.format("d"));
            
            var svg = d3.select($el[0]).append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
            
            // setup tooltip
            var tip = d3.tip()
            .attr('class', 'd3-tip')
            .offset([-10, 0])
            .html(function(d) {
                return "<span style='color:grey'>#" + d.hashtag + " - " + d.y+" </span>";
            })
            svg.call(tip);
            
            // update data from hashtagDateFreq
            $scope.$watch('data', update, true);
            
            function update(hashtagDateFreq){
                
                // fade out bars & axis if just cleared query
                if(hashtagDateFreq.length===0){
                    
                    svg.selectAll("rect, .xaxis, .yaxis")
                    .transition().duration(500)
                    .style("opacity", 1e-6)
                    
                    svg.selectAll("text")
                    .transition().duration(1000)
                    .style("fill", 'transparent')
                    
                    // select all stacks, remove data then remove dom nodes                        
                    window.setTimeout(function(){
                        svg.selectAll('.stack').data([])
                        .exit().remove();
                    },501);
                    
                    return;        
                }
                
                svg.selectAll("rect, .xaxis, .yaxis")
                .transition().duration(500)
                .style("opacity", 1e-6)
                
                // select all stacks, remove data then remove dom nodes                        
                
                svg.selectAll('.xaxis, .yaxis, .stackbar, .stack').data([])
                .exit().remove();
                
                // console.log(hashtagDateFreq);
                
                // generate ["top1","topN"] array
                var numtopN = Object.keys(hashtagDateFreq[0]).length-1;
                var xData=[];
                for(var i=0;i<$scope.max; i++){
                    xData.push("top"+(i+1));
                }
                // console.log(xData);
                
                // format data for stack layout
                // from [ {"day":num, "top1":str, "topN":str},
                //        {"day":num, "top1":str, "topN":str} ]
                // to [ [{x:,y:} //topStack, {x:,y:}//btmStack ],   //1st bar
                //      [{x:,y:} //topStack, {x:,y:}//btmStack ], ] //2ndt bar
                var dataIntermediate = xData.map(function (c) {
                    return hashtagDateFreq.map(function (datum) {
                        return {
                            x: datum.day, 
                            y: datum[c].frequency || 0,
                            hashtag: datum[c].hashtag || ""
                        };
                    });
                });
                
                // console.log(dataIntermediate);
                var dataStackLayout = d3.layout.stack()(dataIntermediate);
                
                x.domain(dataStackLayout[0].map(function (d) {
                    return d.x;
                }));
                
                y.domain([0, d3.max(dataStackLayout[dataStackLayout.length - 1],
                    function (d) { return d.y0 + d.y;})
                ])
                
                // input the data from stack layout
                var layer = svg.selectAll(".stack")
                .data(dataStackLayout)
                .enter().append("g")
                .attr("class", "stack")
                .style("fill", function (d, i) {
                    return color(i);
                })

                // select the data and set y and height to 0
                var rect = layer.selectAll("rect")
                .data(function (d) { return d;})
                .enter()
                .append("g").attr("class", "stackbar")
                .append("rect")
                .attr("x", function (d) { return x(d.x); })
                .attr("y", height)
                .attr("height", 0)
                .attr("width", x.rangeBand())
                .on('mouseover', tip.show)
                .on('mouseout', tip.hide);
                
                // transition rect height from 0 
                rect.transition()
                .delay(function(d, i) { return i * 30; })
                .attr("y", function (d) { return y(d.y + d.y0); })
                .attr("height", function (d) { return y(d.y0) - y(d.y + d.y0);})
                
                // add text label
                var text = layer.selectAll(".stackbar")
                .append("text")
                .style('fill', 'black')
                .transition().duration(500)
                .text(function(d) { if(d.y!==0)return d.y; }) // y is frequency
                .attr("transform", function (d) { 
                    return "translate(" + (x(d.x)+5) + "," + (y(d.y + d.y0)+5) + ")rotate(90)"; 
                })
                .style("text-anchor", "start")
                
                // append and transition axes in 
                svg.append("g")
                .attr("transform", "translate(0," + height + ")")
                .attr("class", "xxis")
                .style("opacity", 1e-6)
                .transition().duration(500)
                .style("opacity", 1)
                .call(xAxis);
                
                svg.append("g")
                .attr("class", "yaxis")
                .transition().duration(500)
                .style("opacity", 1)
                .attr("transform", "translate(" + width + ",0)")
                .call(yAxis);
                
                // axis labels
                svg.selectAll(".xaxis text")  // select all the text elements for the xaxis
                .attr("transform", function(d) {
                    return "translate(" + this.getBBox().height*-2 + "," + this.getBBox().height + ")";
                });
                
                // now add titles to the axes
                svg.append("text")
                .attr("text-anchor", "middle")  // this makes it easy to centre the text as the transform is applied to the anchor
                .attr("transform", "translate("+ (width+padding/3) +","+(height/2)+")rotate(90)")  // text is drawn off the screen top left, move down and out and rotate
                .text("Number of Hashtags");
                
                svg.append("text")
                .attr("text-anchor", "middle")  // this makes it easy to centre the text as the transform is applied to the anchor
                .attr("transform", "translate("+ (width/2) +","+(height+(padding/3))+")")  // centre below axis
                .text("Days before Today");
                
                
            }    
        };
        
        return {
            template:'<div layout="column"><h2 style="text-align:center">{{title}}</h2></div>',
            link: link, 
            restrict: 'E',
            scope:{
                // 2 way bind the data & max, literals for title
                data: '=',
                max: '=',
                title: '@'
            }
        };
        
    }
])
}());