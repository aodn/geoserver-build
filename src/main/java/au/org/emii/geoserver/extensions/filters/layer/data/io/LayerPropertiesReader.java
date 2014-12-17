/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.geotools.util.logging.Logging;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.logging.Logger;

public abstract class LayerPropertiesReader {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters.layer.data.io");

    private DataSource dataSource;
    private String layerName;
    private String schemaName;

    public LayerPropertiesReader(DataSource dataSource, String layerName, String schemaName) {
        this.dataSource = dataSource;
        this.layerName = layerName;
        this.schemaName = schemaName;
    }

    public abstract ArrayList<Filter> read();

    protected DataSource getDataSource() {
        return dataSource;
    }

    protected String getLayerName() {
        return layerName;
    }

    protected String getSchemaName() {
        return schemaName;
    }
}
