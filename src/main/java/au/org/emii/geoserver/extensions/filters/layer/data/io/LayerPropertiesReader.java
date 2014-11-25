/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import au.org.emii.geoserver.extensions.filters.layer.data.LayerIdentifier;
import org.geotools.util.logging.Logging;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.logging.Logger;

public abstract class LayerPropertiesReader {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters.layer.data.io");

    private DataSource dataSource;
    private LayerIdentifier layerIdentifier;

    public LayerPropertiesReader(DataSource dataSource, LayerIdentifier layerIdentifier) {
        this.dataSource = dataSource;
        this.layerIdentifier = layerIdentifier;
    }

    public abstract ArrayList<Filter> read();

    protected DataSource getDataSource() {
        return dataSource;
    }

    protected LayerIdentifier getLayerIdentifier() {
        return layerIdentifier;
    }
}
