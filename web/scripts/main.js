var data = {"book":"The Invisible Man":{"children":[{"children":[{"children":[{"size":8012,"name":"Noun, singular or mass","description":"Noun, singular or mass"}],"name":"Noun, singular or mass","description":"Noun, singular or mass"},{"children":[{"size":2102,"name":"Proper noun, singular","description":"Proper noun, singular"}],"name":"Proper noun, singular","description":"Proper noun, singular"},{"children":[{"size":2002,"name":"Noun, plural","description":"Noun, plural"}],"name":"Noun, plural","description":"Noun, plural"},{"children":[{"size":22,"name":"Proper noun, plural","description":"Proper noun, plural"}],"name":"Proper noun, plural","description":"Proper noun, plural"}],"name":"Nouns","description":"Nouns"},{"children":[{"children":[{"size":4527,"name":"Verb, past tense","description":"Verb, past tense"}],"name":"Verb, past tense","description":"Verb, past tense"},{"children":[{"size":1787,"name":"Verb, base form","description":"Verb, base form"}],"name":"Verb, base form","description":"Verb, base form"},{"children":[{"size":1251,"name":"Verb, past participle","description":"Verb, past participle"}],"name":"Verb, past participle","description":"Verb, past participle"},{"children":[{"size":1217,"name":"Verb, gerund or present participle","description":"Verb, gerund or present participle"}],"name":"Verb, gerund or present participle","description":"Verb, gerund or present participle"},{"children":[{"size":785,"name":"Verb, non-3rd person singular present","description":"Verb, non-3rd person singular present"}],"name":"Verb, non-3rd person singular present","description":"Verb, non-3rd person singular present"},{"children":[{"size":665,"name":"Verb, 3rd person singular present","description":"Verb, 3rd person singular present"}],"name":"Verb, 3rd person singular present","description":"Verb, 3rd person singular present"}],"name":"Verbs","description":"Verbs"},{"children":[{"children":[{"size":3147,"name":"Adverb","description":"Adverb"}],"name":"Adverb","description":"Adverb"},{"children":[{"size":18,"name":"Adverb, superlative","description":"Adverb, superlative"}],"name":"Adverb, superlative","description":"Adverb, superlative"}],"name":"Adverbs","description":"Adverbs"},{"children":[{"children":[{"size":3362,"name":"Adjective","description":"Adjective"}],"name":"Adjective","description":"Adjective"},{"children":[{"size":98,"name":"Adjective, comparative","description":"Adjective, comparative"}],"name":"Adjective, comparative","description":"Adjective, comparative"},{"children":[{"size":47,"name":"Adjective, superlative","description":"Adjective, superlative"}],"name":"Adjective, superlative","description":"Adjective, superlative"}],"name":"Adjectives","description":"Adjectives"},{"children":[{"children":[{"size":4257,"name":"Personal pronoun","description":"Personal pronoun"}],"name":"Personal pronoun","description":"Personal pronoun"},{"children":[{"size":1219,"name":"Possessive pronoun","description":"Possessive pronoun"}],"name":"Possessive pronoun","description":"Possessive pronoun"},{"children":[{"size":209,"name":"Wh-pronoun","description":"Wh-pronoun"}],"name":"Wh-pronoun","description":"Wh-pronoun"},{"children":[{"size":4,"name":"Possessive wh pronoun","description":"Possessive wh pronoun"}],"name":"Possessive wh pronoun","description":"Possessive wh pronoun"}],"name":"Pronouns","description":"Pronouns"},{"children":[{"children":[{"size":6064,"name":"Determiner","description":"Determiner"}],"name":"Determiner","description":"Determiner"},{"children":[{"size":6014,"name":"Preposition or subordinating conjunction","description":"Preposition or subordinating conjunction"}],"name":"Preposition or subordinating conjunction","description":"Preposition or subordinating conjunction"},{"children":[{"size":2620,"name":"Coordinating conjunction","description":"Coordinating conjunction"}],"name":"Coordinating conjunction","description":"Coordinating conjunction"},{"children":[{"size":1070,"name":"to","description":"to"}],"name":"to","description":"to"},{"children":[{"size":628,"name":"Modal","description":"Modal"}],"name":"Modal","description":"Modal"},{"children":[{"size":499,"name":"Cardinal number","description":"Cardinal number"}],"name":"Cardinal number","description":"Cardinal number"},{"children":[{"size":410,"name":"Particle","description":"Particle"}],"name":"Particle","description":"Particle"},{"children":[{"size":206,"name":"Possessive ending","description":"Possessive ending"}],"name":"Possessive ending","description":"Possessive ending"},{"children":[{"size":169,"name":"Wh-determiner","description":"Wh-determiner"}],"name":"Wh-determiner","description":"Wh-determiner"},{"children":[{"size":161,"name":"Existential there","description":"Existential there"}],"name":"Existential there","description":"Existential there"},{"children":[{"size":71,"name":"Predeterminer","description":"Predeterminer"}],"name":"Predeterminer","description":"Predeterminer"},{"children":[{"size":70,"name":"Interjection","description":"Interjection"}],"name":"Interjection","description":"Interjection"},{"children":[{"size":25,"name":"Foreign word","description":"Foreign word"}],"name":"Foreign word","description":"Foreign word"},{"children":[{"size":22,"name":"List item marker","description":"List item marker"}],"name":"List item marker","description":"List item marker"},{"children":[{"size":10,"name":"Symbol","description":"Symbol"}],"name":"Symbol","description":"Symbol"}],"name":"Other","description":"Other"}],"name":"The Invisible Man by H. G. Wells","description":"Parts of Speech"}};


function draw_chart(title) {
    var margin = {top: 260, right: 260, bottom: 260, left: 260},
        radius = Math.min(margin.top, margin.right, margin.bottom, margin.left) - 10;

    function filter_min_arc_size_text(d, i) {return (d.dx*d.depth*radius/3)>14};

    var hue = d3.scale.category10();

    var luminance = d3.scale.sqrt()
        .domain([0, 1e6])
        .clamp(true)
        .range([90, 20]);

    var svg = d3.select("#chart").append("svg")
        .attr("width", margin.left + margin.right)
        .attr("height", margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    var partition = d3.layout.partition()
        .sort(function(a, b) { return d3.ascending(a.name, b.name); })
        .size([2 * Math.PI, radius]);

    var arc = d3.svg.arc()
        .startAngle(function(d) { return d.x; })
        .endAngle(function(d) { return d.x + d.dx - .01 / (d.depth + .5); })
        .innerRadius(function(d) { return radius / 3 * d.depth; })
        .outerRadius(function(d) { return radius / 3 * (d.depth + 1) - 1; });

    //Tooltip description
    var tooltip = d3.select("body")
        .append("div")
        .attr("id", "tooltip")
        .style("position", "absolute")
        .style("z-index", "10")
        .style("opacity", 0);

    function format_number(x) {
        return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    }

    function format_description(d) {
        var description = d.description;
        return   '<b>' + d.name  + '</b><br>(' + format_number(d.value) + ')';
    }

    function computeTextRotation(d) {
        var angle=(d.x +d.dx/2)*180/Math.PI - 90;
        return angle;
    }

    function mouseOverArc(d) {
        d3.select(this).attr("stroke","black")

        tooltip.html(format_description(d));
        return tooltip.transition()
            .duration(50)
            .style("opacity", 0.9);
    }

    function mouseOutArc(){
        d3.select(this).attr("stroke","")
        return tooltip.style("opacity", 0);
    }

    function mouseMoveArc (d) {
        return tooltip
            .style("top", (d3.event.pageY-10)+"px")
            .style("left", (d3.event.pageX+10)+"px");
    }

    var root_ = null;
    console.log(data["name"]);

    root = data["name"][title];
        // Compute the initial layout on the entire tree to sum sizes.
        // Also compute the full name and fill color for each node,
        // and stash the children so they can be restored as we descend.

        partition
            .value(function(d) { return d.size; })
            .nodes(root)
            .forEach(function(d) {
                d._children = d.children;
                d.sum = d.value;
                d.key = key(d);
                d.fill = fill(d);
            });

        // Now redefine the value function to use the previously-computed sum.
        partition
            .children(function(d, depth) { return depth < 2 ? d._children : null; })
            .value(function(d) { return d.sum; });

        var center = svg.append("circle")
            .attr("r", radius / 3)
            .on("click", zoomOut);

        center.append("title")
            .text("Zoom out one level");

        var partitioned_data=partition.nodes(root).slice(1)

        var path = svg.selectAll("path")
            .data(partitioned_data)
            .enter().append("path")
            .attr("d", arc)
            .style("fill", function(d) { return d.fill; })
            .each(function(d) { this._current = updateArc(d); })
            .on("click", zoomIn)
            .on("mouseover", mouseOverArc)
            .on("mousemove", mouseMoveArc)
            .on("mouseout", mouseOutArc);


        var texts = svg.selectAll("text")
            .data(partitioned_data)
            .enter().append("text")
            .filter(filter_min_arc_size_text)
            .attr("transform", function(d) { return "rotate(" + computeTextRotation(d) + ")"; })
            .attr("x", function(d) { return radius / 3 * d.depth; })
            .attr("dx", "6") // margin
            .attr("dy", ".35em") // vertical-align
            .text(function(d,i) {return d.name})

        function zoomIn(p) {
            if (p.depth > 1) p = p.parent;
            if (!p.children) return;
            zoom(p, p);
        }

        function zoomOut(p) {
            if (!p.parent) return;
            zoom(p.parent, p);
        }

        // Zoom to the specified new root.
        function zoom(root, p) {
            if (document.documentElement.__transition__) return;

            // Rescale outside angles to match the new layout.
            var enterArc,
                exitArc,
                outsideAngle = d3.scale.linear().domain([0, 2 * Math.PI]);

            function insideArc(d) {
                return p.key > d.key
                    ? {depth: d.depth - 1, x: 0, dx: 0} : p.key < d.key
                    ? {depth: d.depth - 1, x: 2 * Math.PI, dx: 0}
                    : {depth: 0, x: 0, dx: 2 * Math.PI};
            }

            function outsideArc(d) {
                return {depth: d.depth + 1, x: outsideAngle(d.x), dx: outsideAngle(d.x + d.dx) - outsideAngle(d.x)};
            }

            center.datum(root);

            // When zooming in, arcs enter from the outside and exit to the inside.
            // Entering outside arcs start from the old layout.
            if (root === p) enterArc = outsideArc, exitArc = insideArc, outsideAngle.range([p.x, p.x + p.dx]);

            var new_data=partition.nodes(root).slice(1)

            path = path.data(new_data, function(d) { return d.key; });

            // When zooming out, arcs enter from the inside and exit to the outside.
            // Exiting outside arcs transition to the new layout.
            if (root !== p) enterArc = insideArc, exitArc = outsideArc, outsideAngle.range([p.x, p.x + p.dx]);

            d3.transition().duration(d3.event.altKey ? 7500 : 750).each(function() {
                path.exit().transition()
                    .style("fill-opacity", function(d) { return d.depth === 1 + (root === p) ? 1 : 0; })
                    .attrTween("d", function(d) { return arcTween.call(this, exitArc(d)); })
                    .remove();

                path.enter().append("path")
                    .style("fill-opacity", function(d) { return d.depth === 2 - (root === p) ? 1 : 0; })
                    .style("fill", function(d) { return d.fill; })
                    .on("click", zoomIn)
                    .on("mouseover", mouseOverArc)
                    .on("mousemove", mouseMoveArc)
                    .on("mouseout", mouseOutArc)
                    .each(function(d) { this._current = enterArc(d); });


                path.transition()
                    .style("fill-opacity", 1)
                    .attrTween("d", function(d) { return arcTween.call(this, updateArc(d)); });



            });


            texts = texts.data(new_data, function(d) { return d.key; })

            texts.exit()
                .remove()
            texts.enter()
                .append("text")

        texts.style("opacity", 0)
        .attr("transform", function(d) { return "rotate(" + computeTextRotation(d) + ")"; })
        .attr("x", function(d) { return radius / 3 * d.depth; })
        .attr("dx", "6") // margin
        .attr("dy", ".35em") // vertical-align
        .filter(filter_min_arc_size_text)
        .text(function(d,i) {return d.name})
        .transition().delay(750).style("opacity", 1)

        }

    function key(d) {
        var k = [], p = d;
        while (p.depth) k.push(p.name), p = p.parent;
        return k.reverse().join(".");
    }

    function fill(d) {
        var p = d;
        while (p.depth > 1) p = p.parent;
        var c = d3.lab(hue(p.name));
        c.l = luminance(d.sum);
        return c;
    }

    function arcTween(b) {
        var i = d3.interpolate(this._current, b);
        this._current = i(0);
        return function(t) {
            return arc(i(t));
        };
    }

    function updateArc(d) {
        return {depth: d.depth, x: d.x, dx: d.dx};
    }

    d3.select(self.frameElement).style("height", margin.top + margin.bottom + "px");
}

function change_title_header(title) {
    document.getElementById("titleHeader").innerHTML = title;
}

document.getElementById("book-list").addEventListener("click",function(e) {
    if(e.target && e.target.nodeName == "a") {
        console.log(e.target.id + " was clicked");
    }
});
// JQuery on list item click draw chart and change title