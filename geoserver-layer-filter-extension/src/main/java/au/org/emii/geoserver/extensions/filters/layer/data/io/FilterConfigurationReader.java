/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.FilterConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public class FilterConfigurationReader extends FilterConfigurationIO {

    public FilterConfigurationReader(String dataDirectoryPath) {
        this.dataDirectoryPath = dataDirectoryPath;
    }

    public FilterConfiguration read(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        return new FilterConfiguration(dataDirectoryPath, new FiltersDocumentParser(getDocument(inputStream)).getFilters());
    }

    public Document getDocument(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(inputStream);
    }
}
