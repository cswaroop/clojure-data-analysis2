
(function(window, document, undefined) {
    var margin  = {top: 20, right: 20, bottom: 30, left: 40},
        width   = 960 - margin.left - margin.right,
        height  = 500 - margin.top - margin.bottom,
        x       = d3.scale.linear().range([0, width]),
        y       = d3.scale.linear().range([height, 0]),
        colors  = d3.scale.category10(),
        symbols = d3.svg.symbol(),
        xAxis   = d3.svg.axis().scale(x).orient('bottom'),
        yAxis   = d3.svg.axis().scale(y).orient('left'),
        species = {circle: 'Setosa', cross: 'Versicolor', diamond: 'Virginica'};

    var svg = d3.select('#chart').append('svg')
                .attr('width', width + margin.left + margin.right)
                .attr('height', height + margin.top + margin.bottom)
            .append('g')
                .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

    d3.json('iris-hierarchy.json', function(error, data) {
        x.domain(d3.extent(data, function(d) { return d['petal-width']; }));
        y.domain(d3.extent(data, function(d) { return d['petal-length']; }));

        svg.append('g')
                .attr('class', 'x axis')
                .attr('transform', 'translate(0,' + height + ')')
                .call(xAxis)
            .append('text')
                .attr('class', 'label')
                .attr('x', width)
                .attr('y', -6)
                .style('text-anchor', 'end')
                .text('Petal Width (cm)');

        svg.append('g')
                .attr('class', 'y axis')
                .call(yAxis)
            .append('text')
                .attr('class', 'label')
                .attr('transform', 'rotate(-90)')
                .attr('y', 6)
                .attr('dy', '.71em')
                .attr('text-anchor', 'end')
                .text('Petal Length (cm)')

        svg.selectAll('path')
                .data(data)
            .enter().append('path')
                .attr('d', d3.svg.symbol()
                        .type(function(d) { return d3.svg.symbolTypes[d['class']]; }))
                .attr('transform', function(d) {
                    return 'translate(' + x(d['petal-width']) + ',' +
                        y(d['petal-length']) + ')';
                })
                .style('fill', function(d) { return colors(d['cluster']); });

        var color_legend = svg.selectAll('.legend')
                .data(colors.domain().sort())
            .enter().append('g')
                .attr('class', 'legend')
                .attr('transform', function(d, i) {
                    return 'translate(0,' + (175 + i * 20) + ')';
                });
        color_legend.append('rect')
            .attr('x', width - 18)
            .attr('width', 18)
            .attr('height', 18)
            .style('fill', colors);
        color_legend.append('text')
            .attr('x', width - 24)
            .attr('y', 9)
            .attr('dy', '.35em')
            .style('text-anchor', 'end')
            .text(function(d) { return 'cluster ' + (d + 1); });

        var class_legend = svg.selectAll('.classes')
                .data(['circle', 'cross', 'diamond'])
            .enter().append('g')
                .attr('class', 'classes')
                .attr('transform', function(d, i) {
                    return 'translate(0,' + (275 + i * 20) + ')';
                });
        class_legend.append('path')
            .attr('d', d3.svg.symbol().type(function(d) { return d; }))
            .attr('transform', function(d) {
                return 'translate(' + (width - 9) + ',9)';
            });
        class_legend.append('text')
            .attr('x', width - 18)
            .attr('y', 9)
            .attr('dy', '.35em')
            .style('text-anchor', 'end')
            .text(function(d) { return species[d]; });
    });

})(window, document);

