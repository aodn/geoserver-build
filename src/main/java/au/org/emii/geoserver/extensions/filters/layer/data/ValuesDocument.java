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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Date;

public class ValuesDocument {

    public Document build(Set values) throws Exception {
        Document document = getNewDocument();
        Element valuesElement = document.createElement("uniqueValues");
        document.appendChild(valuesElement);

        for (String value : encodeValues(values)) {
              Element element = document.createElement("value");
              element.appendChild(document.createTextNode(value));
              valuesElement.appendChild(element);
        }
        return document;
    }

    private Document getNewDocument() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        return docBuilder.newDocument();
    }

    private List<String> encodeValues(Set values) {

        List<String> result = new ArrayList<String>();

        if(!values.isEmpty()) {

            Class clazz = values.iterator().next().getClass();

            if (clazz.equals(Boolean.class)) {
                for(Object value : values) {
                    result.add(Boolean.toString((Boolean)value));
                }
            }
            else if (clazz.equals(Integer.class)) {
                for(Object value : values) {
                    result.add(Integer.toString((Integer)value));
                }
            }
            else if (clazz.equals(Long.class)) {
                for(Object value : values) {
                    result.add(Long.toString((Long)value));
                }
            }
            else if (clazz.equals(Float.class)) {
                for(Object value : values) {
                    result.add(Float.toString((Float)value));
                }
            }
            else if (clazz.equals(Double.class)) {
                for(Object value : values) {
                    result.add(Double.toString((Double)value));
                }
            }
            else if (clazz.equals(String.class)) {
                for(Object value : values) {
                    result.add((String)value);
                }
            }
            else if (clazz.equals(java.sql.Date.class)) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                for(Object value : values) {
                    result.add(df.format((Date)value ));
                }
            }
            else {
               throw new RuntimeException("Unrecognized type" );
            }
        }

        return result;
    }
}
