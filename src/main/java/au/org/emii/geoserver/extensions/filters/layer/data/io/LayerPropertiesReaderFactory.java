/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import org.geotools.util.logging.Logging;

import javax.sql.DataSource;
import java.util.logging.Logger;

public class LayerPropertiesReaderFactory {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters.layer.data.io");

    public static LayerPropertiesReader getReader(DataSource dataSource, String layerName, String schemaName) {
        String ciDriverName = DatabaseDriverName.getName(dataSource, layerName, schemaName).toLowerCase();
        if (ciDriverName.contains("postgres")) {
            return new PostgresSqlLayerPropertiesReader(dataSource, layerName, schemaName);
        }
        // Your driver implementation returned here

        return new NullLayerPropertiesReader();
    }
}
