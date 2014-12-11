/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;

import javax.sql.DataSource;
import java.util.List;

public class NullLayerPropertiesReader implements LayerPropertiesReader {

    public List<Filter> read() {
        throw new RuntimeException("No LayerPropertiesReader found for your JDBC driver");
    }
}
