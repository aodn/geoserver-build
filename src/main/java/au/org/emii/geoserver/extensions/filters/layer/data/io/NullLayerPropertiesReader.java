/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import au.org.emii.geoserver.extensions.filters.layer.data.LayerIdentifier;

import javax.sql.DataSource;
import java.util.ArrayList;

public class NullLayerPropertiesReader extends LayerPropertiesReader {

    public NullLayerPropertiesReader(DataSource dataSource, LayerIdentifier layerIdentifier) {
        super(dataSource, layerIdentifier);
    }

    public ArrayList<Filter> read() {
        throw new RuntimeException("No LayerPropertiesReader found for your JDBC driver");
    }
}
