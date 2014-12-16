/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.apache.commons.dbutils.DbUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PostgresSqlLayerPropertiesReader extends LayerDataReader implements LayerPropertiesReader {

    public PostgresSqlLayerPropertiesReader(DataSource dataSource, String layerName, String schemaName) {
        super(dataSource, layerName, schemaName);
    }

    public List<Filter> read() {
        List<Filter> layerTableProperties = new ArrayList<Filter>();

        Connection connection = null;
        try {
            connection = getDataSource().getConnection();
            setSearchPath(connection);
            layerTableProperties = getLayerTableProperties(connection);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        finally {
            DbUtils.closeQuietly(connection);
        }

        return layerTableProperties;
    }

    private void setSearchPath(Connection connection) throws SQLException {
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement("set search_path to ?");
            statement.setString(1, getSchemaName());
        }
        finally {
            DbUtils.closeQuietly(statement);
        }
    }

    private List<Filter> getLayerTableProperties(Connection connection) throws SQLException {
        PreparedStatement statement = null;
        ResultSet results = null;

        try {
            statement = connection.prepareStatement("select column_name, data_type, character_maximum_length from information_schema.columns where table_name = ?");
            statement.setString(1, getLayerName());

            return buildFilters(statement.executeQuery());
        }
        finally {
            DbUtils.closeQuietly(results);
            DbUtils.closeQuietly(statement);
        }
    }

    private List<Filter> buildFilters(ResultSet results) throws SQLException {
        ArrayList<Filter> filters = new ArrayList<Filter>();
        while (results.next()) {
            filters.add(new Filter(results.getString("column_name"), translateDataType(results.getString("data_type"))));
        }

        return filters;
    }

    private String translateDataType(String type) {
        String translation = type.toLowerCase();
        if (translation.startsWith("character")) {
            translation = "string";
        }
        else if (translation.startsWith("double")) {
            translation = "double";
        }
        else if (translation.startsWith("timestamp")) {
            translation = "dateTime";
        }
        return translation;
    }
}
