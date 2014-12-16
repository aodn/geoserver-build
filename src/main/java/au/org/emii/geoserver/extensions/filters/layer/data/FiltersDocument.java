/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

public class FiltersDocument {

    public void build(Document document, List<Filter> filters) {

        Element filtersElement = appendChild(document, document, "filters");

        for (Filter filter : filters) {
            Element filterElement = appendChild(document, filtersElement, "filter");

            appendChild(document, filterElement, "name").appendChild(document.createTextNode(filter.getName()));
            appendChild(document, filterElement, "type").appendChild(document.createTextNode(filter.getType()));
            appendChild(document, filterElement, "label").appendChild(document.createTextNode(filter.getLabel()));
            appendChild(document, filterElement, "visualised").appendChild(document.createTextNode(filter.getVisualised().toString()));

            Element values = appendChild(document, filterElement, "values");
            appendPossibleValues(filter, document, values);
        }
    }

    private Element appendChild(Document document, Node parent, String name) {
        Element element = document.createElement(name);
        parent.appendChild(element);

        return element;
    }

    private void appendPossibleValues(Filter filter, Document document, Node parent) {
        if (filter.getPossibleValues() != null) {
            for (String value : filter.getPossibleValues()) {
                appendChild(document, parent, "value").appendChild(document.createTextNode(value));
            }
        }
    }
}
