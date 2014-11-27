/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import org.apache.commons.dbutils.DbUtils;
import org.geotools.util.logging.Logging;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayerPropertiesReaderFactory {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters.layer.data.io");

    public static LayerPropertiesReader getReader(DataSource dataSource, String layerName, String schemaName) {
        String driverName = "";
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            driverName = connection.getMetaData().getDriverName();
            LOGGER.log(Level.INFO, String.format("DataSource driver name is %s", driverName));
        }
        catch (SQLException e) {
            LOGGER.log(Level.SEVERE, String.format("Error reading driver name %s", e.toString()), e);
        }
        finally {
            DbUtils.closeQuietly(connection);
        }

        return getLayerPropertiesReaderForDriver(driverName, dataSource, layerName, schemaName);
    }

    private static LayerPropertiesReader getLayerPropertiesReaderForDriver(
        String driverName,
        DataSource dataSource,
        String layerName,
        String schemaName
    )
    {
        String ciDriverName = driverName.toLowerCase();
        if (ciDriverName.contains("postgres")) {
            return new PostgresSqlLayerPropertiesReader(dataSource, layerName, schemaName);
        }
        // Your driver implementation returned here

        return new NullLayerPropertiesReader(dataSource, layerName, schemaName);
    }
}
