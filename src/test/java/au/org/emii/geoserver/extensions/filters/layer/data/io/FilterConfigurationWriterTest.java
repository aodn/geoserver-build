/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import freemarker.template.TemplateException;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FilterConfigurationWriterTest {

    private static String NO_FILTERS_XML = "<?xml version=\"1.0\"?>\n" +
        "<filters>\n" +
        "</filters>\n";

    private static String FILTERS_XML = "<?xml version=\"1.0\"?>\n" +
        "<filters>\n" +
        "    <filter>\n" +
        "        <name>file_id</name>\n" +
        "        <type>integer</type>\n" +
        "        <label>You</label>\n" +
        "        <visualised>false</visualised>\n" +
        "        <excludedFromDownload>false</excludedFromDownload>\n" +
        "    </filter>\n" +
        "    <filter>\n" +
        "        <name>url</name>\n" +
        "        <type>character varying</type>\n" +
        "        <label>Bet</label>\n" +
        "        <visualised>true</visualised>\n" +
        "        <excludedFromDownload>true</excludedFromDownload>\n" +
        "        <arbitraryfield>TEST</arbitraryfield>\n" +
        "    </filter>\n" +
        "</filters>\n";

    @Test
    public void writeNoFiltersTest() throws TemplateException, IOException {
        FilterConfigurationWriter filterConfigurationWriter = new FilterConfigurationWriter(new ArrayList<Filter>());
        StringWriter writer = new StringWriter();
        filterConfigurationWriter.write(writer);

        assertEquals(NO_FILTERS_XML, writer.toString());
    }

    @Test
    public void writeOnlyEnabledTest() throws TemplateException, IOException {
        FilterConfigurationWriter filterConfigurationWriter = new FilterConfigurationWriter(getFilters());
        StringWriter writer = new StringWriter();
        filterConfigurationWriter.write(writer);

        assertEquals(FILTERS_XML, writer.toString());
    }

    private List<Filter> getFilters() {
        List<Filter> filters = new ArrayList<Filter>(3);
        filters.add(buildFilter("file_id", "integer", "You", Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE));
        filters.add(buildFilter("no", "not this", "one", Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE));
        filters.add(buildFilter("url", "character varying", "Bet", Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE));
        return filters;
    }

    private Filter buildFilter(String name, String type, String label, Boolean visualised, Boolean enabled, Boolean excludedFromDownload, Boolean addsomeextras) {
        Filter filter = new Filter(name, type);
        filter.setLabel(label);
        filter.setVisualised(visualised);
        filter.setEnabled(enabled);
        filter.setExcludedFromDownload(excludedFromDownload);
        if (addsomeextras) {
            filter.setExtrasField("TEST", "arbitraryfield");
        }

        return filter;
    }
}
