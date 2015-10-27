/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.util.Set;

public class ValuesDocument {

    public Document build(Set values) throws Exception {
        Document document = getNewDocument();
        Element valuesElement = document.createElement("uniqueValues");
        document.appendChild(valuesElement);

        for (String encodedValue : new ValueEncoder().encode(values)) {
              Element element = document.createElement("value");
              element.appendChild(document.createTextNode(encodedValue));
              valuesElement.appendChild(element);
        }
        return document;
    }

    private Document getNewDocument() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        return docBuilder.newDocument();
    }
}
