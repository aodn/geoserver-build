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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.BeansWrapper;

public class FilterConfigurationWriter extends FilterConfigurationIO {

    private static final String TEMPLATE_NAME = "filter_configuration.ftl";

    private List<Filter> filters;

    public FilterConfigurationWriter(List<Filter> filters) {
        this.filters = filters;
    }

    public void write(Writer writer) throws TemplateException, IOException {
        Configuration config = new Configuration();
        config.setClassForTemplateLoading(FilterConfigurationWriter.class, "");

        Template template = config.getTemplate(TEMPLATE_NAME);
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("filters", new CollectionModel(getEnabledFilters(), BeansWrapper.getDefaultInstance()));
        template.process(root, writer);
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
