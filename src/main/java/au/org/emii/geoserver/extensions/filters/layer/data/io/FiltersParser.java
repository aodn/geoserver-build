/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class FiltersParser {

    private NodeWrapper filtersNode;

    public FiltersParser(Node filtersNode) {
        this.filtersNode = new NodeWrapper(filtersNode);
    }

    public List<Filter> parse() {
        List<Filter> filters = new ArrayList<Filter>();
        for (Node node : filtersNode) {
            addFilter(filters, node);
        }

        return filters;
    }

    private void addFilter(List<Filter> filters, Node node) {
        if (isFilterNode(node)) {
            FilterParser filterParser = new FilterParser(node);
            filters.add(filterParser.parse());
        }
    }

    private boolean isFilterNode(Node node) {
        return isElement(node) && isName(node);
    }

    private boolean isElement(Node node) {
        return Node.ELEMENT_NODE == node.getNodeType();
    }

    private boolean isName(Node node) {
        return "filter".equals(node.getNodeName());
    }
}
