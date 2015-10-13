/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

public class FiltersDocumentParser {

    private Document document;

    public FiltersDocumentParser(Document document) {
        this.document = document;
    }

    public List<Filter> getFilters() {
        FiltersParser filtersParser = new FiltersParser(getFiltersNode());
        return filtersParser.parse();
    }

    private Node getFiltersNode() {
        return document.getFirstChild();
    }
}
