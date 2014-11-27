/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;

import javax.sql.DataSource;
import java.util.ArrayList;

public class NullLayerPropertiesReader extends LayerPropertiesReader {

    public NullLayerPropertiesReader(DataSource dataSource, String layerName, String schemaName) {
        super(dataSource, layerName, schemaName);
    }

    public ArrayList<Filter> read() {
        throw new RuntimeException("No LayerPropertiesReader found for your JDBC driver");
    }
}
