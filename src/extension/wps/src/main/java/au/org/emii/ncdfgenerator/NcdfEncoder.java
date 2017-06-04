package au.org.emii.ncdfgenerator;

import org.geotools.data.postgis.PostGISDialect;
import org.geotools.data.postgis.PostgisFilterToSQL;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFileWriteable;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NcdfEncoder implements AutoCloseable {
    private final Connection conn;
    private final JDBCDataStore dataStore;
    private final String schema;
    private final ICreateWritable createWritable;
    private final NcdfDefinition definition;
    private final String filterExpr;
    private static final Logger logger = LoggerFactory.getLogger(NcdfEncoder.class);
    private final IAttributeValueParser attributeValueParser;
    private final int fetchSize;

    private PreparedStatement featuresStmt;
    private ResultSet featureInstancesRS;
    private String whereClause;
    private String orderClause;

    private IOutputFormatter outputFormatter;

    public NcdfEncoder(
            JDBCDataStore dataStore,
            String schema,
            ICreateWritable createWritable,
            IAttributeValueParser attributeValueParser,
            NcdfDefinition definition,
            String filterExpr
    ) throws SQLException {
        this.dataStore = dataStore;
        this.conn = dataStore.getDataSource().getConnection();
        this.schema = schema;
        this.createWritable = createWritable;
        this.attributeValueParser = attributeValueParser;
        this.definition = definition;
        this.filterExpr = filterExpr;

        fetchSize = 10000;
        outputFormatter = null;
        featuresStmt = null;
        featureInstancesRS = null;
    }

    private String getVirtualDataTable() {
        return definition.getDataSource().getVirtualDataTable();
    }

    private String getVirtualInstanceTable() {
        return definition.getDataSource().getVirtualInstanceTable();
    }

    private String getVirtualTable() {
        return "select *"
            + " from (" + getVirtualDataTable() + ") as data"
            + " join (" + getVirtualInstanceTable() + ") instance"
            + " on instance.id = data.instance_id";
    }

    public void prepare(IOutputFormatter outputFormatter) throws Exception {
        this.outputFormatter = outputFormatter;

        // do not quote search path!.
        try (PreparedStatement pathStmt = conn.prepareStatement("set search_path=" + schema + ", public")) {
            pathStmt.execute();
        }

        // Batch results set
        conn.setAutoCommit(false);

        // if we combine both tables, then it's actually simpler, since don't need to process twice
        // or discriminate about which attributes come from which tables.
        // And there's no optimisation penalty since both the initial and instance queries have to hit the big data table
        String instanceQuery =
                "select distinct data.instance_id"
                        + " from (" + getVirtualDataTable() + ") as data"
                        + " join (" + getVirtualInstanceTable() + ") instance"
                        + " on instance.id = data.instance_id";

        instanceQuery = applyFilter(instanceQuery);

        instanceQuery += ";";

        PreparedStatement featuresStmt = conn.prepareStatement(instanceQuery);
        featuresStmt.setFetchSize(fetchSize);

        // change name featureInstancesRSToProcess ?
        featureInstancesRS = featuresStmt.executeQuery();
    }

    private String applyFilter(String query) throws Exception {
        if (filterExpr != null) {
            Filter filter = CQL.toFilter(filterExpr);
            SimpleFeatureType featureType = FeatureTypeFactory.getFeatureType(dataStore, getVirtualTable(), true);

            PostgisFilterToSQL sqlEncoder = new PostgisFilterToSQL(new PostGISDialect(null));
            sqlEncoder.setSqlNameEscape("\"");
            sqlEncoder.setFeatureType(featureType);

            whereClause = sqlEncoder.encodeToString(filter);
            if(whereClause != null) {
                query += " " + whereClause;
            }
        }

        return query;
    }

    public boolean writeNext() throws Exception {
        if (!featureInstancesRS.next()) {
            logger.info("no more instances");
            if (outputFormatter != null) {
                logger.info("closing outputFormatter");
                outputFormatter.close();
                outputFormatter = null;
            }
            return false;
        }

        long instanceId = featureInstancesRS.getLong(1);

        orderClause = "";
        for (IDimension dimension : definition.getDimensions()) {
            if (!orderClause.equals("")) {
                orderClause += ",";
            }
            orderClause += "\"" + dimension.getName() + "\"";
        }

        String query =
                getVirtualTable()
                    + (whereClause != null ? " " + whereClause : "")
                    + " and instance.id = " + Long.toString(instanceId)
                    + " order by " + orderClause
                    + ";";

        logger.info("instanceId " + instanceId + ", " + query);

        populateValues(query, definition.getDimensions(), definition.getVariables());

        NetcdfFileWriteable writer = createWritable.create();

        // Write the global attributes
        for (Attribute attribute : definition.getGlobalAttributes()) {
            String name = attribute.getName();
            Object value = null;

            if (attribute.getValue() != null) {
                value = attributeValueParser.parse(attribute.getValue()).getValue();
            }
            else if (attribute.getSql() != null) {
                String replacedSql = markupSql(instanceId, whereClause, orderClause, attribute.getSql());
                value = evaluateSql(replacedSql);
            }
            else {
                throw new NcdfGeneratorException("No value defined for global attribute '" + name + "'");
            }

            if (value == null) {
                logger.warn("Null attribute value '" + name + "'");
            }
            else if (value instanceof Number) {
                writer.addGlobalAttribute(name, (Number) value);
            }
            else if (value instanceof String) {
                writer.addGlobalAttribute(name, (String) value);
            }
            else if (value instanceof Array) {
                writer.addGlobalAttribute(name, (Array) value);
            }
            else {

                String errmsg = String.format("XML attribute error: Type:%s Contains:%s", value.getClass().getName(), attribute.getValue());
                throw new NcdfGeneratorException(errmsg);
            }
        }

        // define dimensions
        for (IDimension dimension : definition.getDimensions()) {
            dimension.define(writer);
        }

        // define vars
        for (IVariable variable : definition.getVariables()) {
            variable.define(writer);
        }

        // finish netcdf definition
        writer.create();

        // write values
        for (IVariable variable : definition.getVariables()) {
            // maybe change name writeValues
            variable.finish(writer);
        }
        // finish the file
        writer.close();

        String replacedSql = markupSql(instanceId, whereClause, orderClause, definition.getFilenameTemplate().getSql());
        // get filename
        Object filename = evaluateSql(replacedSql);

        try (InputStream is = createWritable.getStream()) {
            outputFormatter.write((String) filename, is);
        }
        catch (Exception e) {
            String err = String.format("Error writing netCDF filename for <virtualDataTable> instance_id %s \nSQL:\n%s", Long.toString(instanceId), replacedSql);
            throw new NcdfGeneratorException(err);
        }

        return true;
    }

    private String markupSql(long instanceId, String whereClause, String orderClause, String sql) {

        // we need aliases for the inner select, and to support wrapping the where selection
        sql = sql.replaceAll("\\$instance",
                "( select *"
                    + " from (" + getVirtualInstanceTable() + ") instance "
                    + " where instance.id = " + Long.toString(instanceId) + ") as instance "
        );

        // as for vars/dims, but without the order clause, to support aggregate functions like min/max
        sql = sql.replaceAll("\\$data",
                "( select *"
                    + " from (" + getVirtualDataTable() + ") as data"
                    + " join (" + getVirtualInstanceTable() + ") instance"
                    + " on instance.id = data.instance_id"
                    + " " + whereClause
                    + " and instance.id = " + Long.toString(instanceId)
                    + " order by " + orderClause
                    + " ) as data"
        );
        return sql;
    }

    private Object evaluateSql(String sql) throws Exception {

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setFetchSize(fetchSize);
            rs = stmt.executeQuery();

            // TODO more checks around this
            // maybe support converion to ncdf array attribute types
            rs.next();
            return rs.getObject(1);
        }
        finally {
            if (stmt != null) {
                stmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    private void populateValues(
            String query,
            List<IDimension> dimensions,
            List<IVariable> encoders
    ) throws Exception {
        // prepare buffers
        for (IDimension dimension : definition.getDimensions()) {
            dimension.prepare();
        }

        for (IVariable variable : definition.getVariables()) {
            variable.prepare();
        }

        // sql stuff
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setFetchSize(fetchSize);

            try (ResultSet rs = stmt.executeQuery()) {

                // now we loop the main attributes
                ResultSetMetaData m = rs.getMetaData();
                int numColumns = m.getColumnCount();

                // organize dimensions by name
                Map<String, IDimension> dimensionsMap = new HashMap<String, IDimension>();
                for (IDimension dimension : definition.getDimensions()) {
                    dimensionsMap.put(dimension.getName(), dimension);
                }

                // organize variables by name
                Map<String, IVariable> variablesMap = new HashMap<String, IVariable>();
                for (IVariable variable : definition.getVariables()) {
                    variablesMap.put(variable.getName(), variable);
                }

                // pre-map the encoders by index according to the column name
                ArrayList<IAddValue>[] processing = (ArrayList<IAddValue>[]) new ArrayList[numColumns + 1];

                for (int i = 1; i <= numColumns; ++i) {

                    processing[i] = new ArrayList<IAddValue>();

                    IDimension dimension = dimensionsMap.get(m.getColumnName(i));
                    if (dimension != null) {
                        processing[i].add(dimension);
                    }

                    IAddValue variable = variablesMap.get(m.getColumnName(i));
                    if (variable != null) {
                        processing[i].add(variable);
                    }
                }

                // process result set rows
                while (rs.next()) {
                    for (int i = 1; i <= numColumns; ++i) {
                        for (IAddValue p : processing[i]) {
                            p.addValueToBuffer(rs.getObject(i));
                        }
                    }
                }
            }
       }
    }

    public void close() {
        closeQuietly(featureInstancesRS);
        closeQuietly(featuresStmt);
        closeQuietly(conn);
    }

    private void closeQuietly(AutoCloseable autoCloseable) {
        if (autoCloseable == null) return;

        try {
            autoCloseable.close();
        } catch (Exception e) {
            logger.warn(String.format("Problem closing %s", autoCloseable.getClass().getCanonicalName()), e);
        }
    }
}
