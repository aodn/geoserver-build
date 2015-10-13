/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.FilterConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class FilterConfigurationReaderTest {

    private static String XML = "<?xml version=\"1.0\"?>\n" +
        "<filters>\n" +
        "    <filter>\n" +
        "        <name>file_id</name>\n" +
        "        <type>integer</type>\n" +
        "        <label>You</label>\n" +
        "        <visualised>false</visualised>\n" +
        "    </filter>\n" +
        "    <filter>\n" +
        "        <name>url</name>\n" +
        "        <type>character varying</type>\n" +
        "        <label>Bet</label>\n" +
        "        <visualised>true</visualised>\n" +
        "    </filter>\n" +
        "    <filter>\n" +
        "        <name>size</name>\n" +
        "        <type>double precision</type>\n" +
        "        <label>This</label>\n" +
        "        <visualised>true</visualised>\n" +
        "    </filter>\n" +
        "    <filter>\n" +
        "        <name>deployment_name</name>\n" +
        "        <type>character varying</type>\n" +
        "        <label>Happens</label>\n" +
        "        <visualised>true</visualised>\n" +
        "        <excludedFromDownload>true</excludedFromDownload>\n" +
        "    </filter>\n" +
        "</filters>\n";

    @Test
    public void readTest() throws ParserConfigurationException, SAXException, IOException {
        InputStream stream = null;

        FilterConfigurationReader reader = new FilterConfigurationReader("");

        try {
            stream = new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8));
            FilterConfiguration filterConfiguration = reader.read(stream);

            assertEquals(4, filterConfiguration.getFilters().size());
            assertEquals("integer", filterConfiguration.getFilters().get(0).getType());
            assertEquals(Boolean.FALSE, filterConfiguration.getFilters().get(0).getVisualised());
            assertEquals(Boolean.TRUE, filterConfiguration.getFilters().get(1).getVisualised());
            assertEquals("This", filterConfiguration.getFilters().get(2).getLabel());
            assertEquals("deployment_name", filterConfiguration.getFilters().get(3).getName());
            assertEquals(Boolean.TRUE, filterConfiguration.getFilters().get(3).getExcludedFromDownload());
        }
        finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
