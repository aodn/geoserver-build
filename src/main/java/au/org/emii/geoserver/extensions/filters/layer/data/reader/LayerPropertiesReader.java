/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.reader;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import au.org.emii.geoserver.extensions.filters.layer.data.LayerIdentifier;
import org.apache.commons.dbutils.DbUtils;
import org.geotools.util.logging.Logging;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayerPropertiesReader {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters.layer.data.reader");

    private DataSource dataSource;
    private LayerIdentifier layerIdentifier;

    public LayerPropertiesReader(DataSource dataSource, LayerIdentifier layerIdentifier) {
        this.dataSource = dataSource;
        this.layerIdentifier = layerIdentifier;
    }

    public ArrayList<Filter> read() {
        ArrayList<Filter> layerDataProperties = new ArrayList<Filter>();

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            setSearchPath(connection);
            layerDataProperties = getLayerTableProperties(connection);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        finally {
            DbUtils.closeQuietly(connection);
        }

        return layerDataProperties;
    }

    private void setSearchPath(Connection connection) throws SQLException {
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement("set search_path to ?");
            statement.setString(1, layerIdentifier.getSchemaName());
        }
        finally {
            DbUtils.closeQuietly(statement);
        }
    }

    private ArrayList<Filter> getLayerTableProperties(Connection connection) throws SQLException {
        PreparedStatement statement = null;
        ResultSet results = null;

        try {
            statement = connection.prepareStatement("select column_name, data_type, character_maximum_length from INFORMATION_SCHEMA.COLUMNS where table_name = ?");
            statement.setString(1, layerIdentifier.getLayerName());

            return buildFilters(statement.executeQuery());
        }
        finally {
            DbUtils.closeQuietly(results);
            DbUtils.closeQuietly(statement);
        }
    }

    private ArrayList<Filter> buildFilters(ResultSet results) throws SQLException {
        ArrayList<Filter> filters = new ArrayList<Filter>();
        while (results.next()) {
            filters.add(new Filter(results.getString("column_name"), results.getString("data_type")));
        }

        return filters;
    }
}
