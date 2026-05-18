const graphElement = document.getElementById('rdfGraph');

if (graphElement) {
    const nodes = JSON.parse(graphElement.dataset.nodes || '[]');
    const links = JSON.parse(graphElement.dataset.edges || '[]');
    const width = graphElement.clientWidth || 1000;
    const height = Math.max(560, Math.min(900, nodes.length * 70));

    const svg = d3.select(graphElement)
        .append('svg')
        .attr('viewBox', [0, 0, width, height])
        .attr('preserveAspectRatio', 'xMidYMid meet');

    svg.append('defs')
        .append('marker')
        .attr('id', 'd3Arrow')
        .attr('viewBox', '0 -5 10 10')
        .attr('refX', 30)
        .attr('refY', 0)
        .attr('markerWidth', 8)
        .attr('markerHeight', 8)
        .attr('orient', 'auto')
        .append('path')
        .attr('d', 'M0,-5L10,0L0,5')
        .attr('fill', '#7c6df0');

    const simulation = d3.forceSimulation(nodes)
        .force('link', d3.forceLink(links).id(d => d.id).distance(190).strength(0.65))
        .force('charge', d3.forceManyBody().strength(-520))
        .force('center', d3.forceCenter(width / 2, height / 2))
        .force('collision', d3.forceCollide().radius(72));

    const link = svg.append('g')
        .selectAll('line')
        .data(links)
        .join('line')
        .attr('class', 'd3-link')
        .attr('marker-end', 'url(#d3Arrow)');

    const edgeLabel = svg.append('g')
        .selectAll('text')
        .data(links)
        .join('text')
        .attr('class', 'd3-edge-label')
        .text(d => d.label);

    const node = svg.append('g')
        .selectAll('g')
        .data(nodes)
        .join('g')
        .attr('class', d => `d3-node ${d.type}`)
        .call(d3.drag()
            .on('start', dragstarted)
            .on('drag', dragged)
            .on('end', dragended));

    node.append('circle')
        .attr('r', d => d.type === 'resource' ? 42 : 36);

    node.append('text')
        .attr('text-anchor', 'middle')
        .attr('dominant-baseline', 'middle')
        .text(d => d.id.length > 18 ? d.id.slice(0, 16) + '…' : d.id);

    node.append('title')
        .text(d => d.id);

    simulation.on('tick', () => {
        nodes.forEach(d => {
            d.x = Math.max(60, Math.min(width - 60, d.x));
            d.y = Math.max(60, Math.min(height - 60, d.y));
        });

        link
            .attr('x1', d => d.source.x)
            .attr('y1', d => d.source.y)
            .attr('x2', d => d.target.x)
            .attr('y2', d => d.target.y);

        edgeLabel
            .attr('x', d => (d.source.x + d.target.x) / 2)
            .attr('y', d => (d.source.y + d.target.y) / 2);

        node.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    function dragstarted(event, d) {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }

    function dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }

    function dragended(event, d) {
        if (!event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }
}
