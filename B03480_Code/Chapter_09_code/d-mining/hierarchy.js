
function graphClusters(clusterFile, height) {
    var width  = 960;

    var cluster = d3.layout.cluster()
            .size([height, width-160]);
    var diagonal = d3.svg.diagonal()
            .projection(function(d) { return [d.y, d.x]; });
    var svg = d3.select('body').append('svg')
            .attr('width', width)
            .attr('height', height)
            .append('g')
            .attr('transform', 'translate(40,0)')

    d3.json(clusterFile, function(error, root) {
        var nodes = cluster.nodes(root),
            links = cluster.links(nodes);

        var node = svg.selectAll('.node')
                .data(nodes)
            .enter().append('g')
                .attr('class', 'node')
                .attr('transform', function(d) {
                    return 'translate(' + d.y + ',' + d.x + ')';
                });
                node.append('circle').attr('r', 4.5);

        var link = svg.selectAll('link')
                .data(links)
            .enter().append('path')
                .attr('class', 'link')
                .attr('d', diagonal);

        node.append('text')
                .attr('dx', function(d) {
                    return d.children.length > 0 ? -8 : 8;
                })
                .attr('dy', 3)
                .style('text-anchor', function(d) {
                    return d.children.length > 0 ? "end" : "start";
                })
                .text(function(d) { return d.name; });
    });

    d.select(self.frameElement).style('height', height + 'px');
}

