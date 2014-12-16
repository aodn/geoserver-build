package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.dbutils.DbUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PostgresSqlPossibleValuesReader extends LayerDataReader implements PossibleValuesReader {

    public PostgresSqlPossibleValuesReader(DataSource dataSource, String layerName, String schemaName) {
        super(dataSource, layerName, schemaName);
    }

    public void read(List<Filter> filters) {
        Connection connection = null;
        try {
            connection = getDataSource().getConnection();
            setSearchPath(connection);
            for (Filter filter : getPossibleValueFilters(filters)) {
                filter.setPossibleValues(getPossibleValues(connection, getLayerName(), filter.getName()));
            }
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        finally {
            DbUtils.closeQuietly(connection);
        }
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

    private List<String> getPossibleValues(Connection connection, String table, String column) throws SQLException {
        List<String> possibleValues = new ArrayList<String>();
        PreparedStatement statement = null;
        ResultSet results = null;

        try {
            statement = connection.prepareStatement(String.format("select distinct %s from %s.%s", column, getSchemaName(), table));
            results = statement.executeQuery();
            while (results.next()) {
                possibleValues.add(results.getString(column));
            }
        }
        finally {
            DbUtils.closeQuietly(results);
            DbUtils.closeQuietly(statement);
        }

        return possibleValues;
    }

    private List<Filter> getPossibleValueFilters(List<Filter> filters) {
        List<Filter> possibleValueFilters = new ArrayList<Filter>(filters);
        CollectionUtils.filter(possibleValueFilters, getPredicate());
        return possibleValueFilters;
    }

    private Predicate getPredicate() {
        return new Predicate() {
            public boolean evaluate(Object o) {
                return "string".equals(((Filter)o).getType().toLowerCase());
            }
        };
    }
}
