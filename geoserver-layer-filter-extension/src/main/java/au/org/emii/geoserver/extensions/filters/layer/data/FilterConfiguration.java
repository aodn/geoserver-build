/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import java.io.Serializable;
import java.util.List;

public class FilterConfiguration implements Serializable {

    private List<Filter> filters;
    private String dataDirectory;

    public FilterConfiguration(String dataDirectory, List<Filter> filters) {
        this.dataDirectory = dataDirectory;
        this.filters = filters;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }
}
