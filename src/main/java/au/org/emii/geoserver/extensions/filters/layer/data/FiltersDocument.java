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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

public class FiltersDocument {

    public Document build(List<Filter> filters) throws ParserConfigurationException {
        Document document = getNewDocument();
        Element filtersElement = appendChild(document, document, "filters");

        for (Filter filter : filters) {
            appendFilter(document, filtersElement, filter);
        }

        return document;
    }

    private Document getNewDocument() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        return docBuilder.newDocument();
    }

    private void appendFilter(Document document, Node filtersElement, Filter filter) {
        Element filterElement = appendChild(document, filtersElement, "filter");

        appendChild(document, filterElement, "name").appendChild(document.createTextNode(filter.getName()));
        appendChild(document, filterElement, "type").appendChild(document.createTextNode(filter.getType()));
        appendChild(document, filterElement, "label").appendChild(document.createTextNode(filter.getLabel()));
        appendChild(document, filterElement, "visualised").appendChild(document.createTextNode(filter.getVisualised().toString()));

        appendPossibleValues(filter, document, appendChild(document, filterElement, "values"));
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
