/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.IOUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterConfigurationWriter {

    private static final String TEMPLATE_NAME = "filter_configuration.ftl";
    private static final String OUTPUT_FILE_NAME = "filters.xml";

    private String dataDirectoryPath;
    private List<Filter> filters;

    public FilterConfigurationWriter(String dataDirectoryPath, List<Filter> filters) {
        this.dataDirectoryPath = dataDirectoryPath;
        this.filters = filters;
    }

    public void write() {
        Configuration config = new Configuration();
        config.setClassForTemplateLoading(FilterConfigurationWriter.class, "");

        FileWriter writer = null;
        try {
            Template template = config.getTemplate(TEMPLATE_NAME);
            writer = new FileWriter(String.format("%s/%s", dataDirectoryPath, OUTPUT_FILE_NAME));

            Map<String, Object> root = new HashMap<String, Object>();
            root.put("filters", getEnabledFilters());
            template.process(root, writer);

            // Probably throw the exceptions so errors are reported
        }
        catch (TemplateException te) {
            te.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private List<Filter> getEnabledFilters() {
        List<Filter> enabledFilters = new ArrayList<Filter>(filters);
        CollectionUtils.filter(enabledFilters, getPredicate());
        return enabledFilters;
    }

    private Predicate getPredicate() {
        return new Predicate() {
            public boolean evaluate(Object o) {
                return ((Filter)o).getEnabled().booleanValue();
            }
        };
    }
}
