package au.org.emii.ncdfgenerator;

import au.org.emii.ncdfgenerator.cql.IDialectTranslate;
import au.org.emii.ncdfgenerator.cql.IExprParser;
import au.org.emii.ncdfgenerator.cql.IExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFileWriteable;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO ucar.nc2.NetcdfFile

public class NcdfEncoder {
    private final IExprParser exprParser;
    private final IDialectTranslate translate;
    private final Connection conn;
    private final String schema;
    private final ICreateWritable createWritable;
    private final NcdfDefinition definition;
    private final String filterExpr;
    private static final Logger logger = LoggerFactory.getLogger(NcdfEncoder.class);
    private final IAttributeValueParser attributeValueParser;
    private final int fetchSize;

    private ResultSet featureInstancesRS;
    private String selectionClause ;
    private String orderClause;

    private IOutputFormatter outputFormatter;

    public NcdfEncoder(
        IExprParser exprParser,
        IDialectTranslate translate,
        Connection conn,
        String schema,
        ICreateWritable createWritable,
        IAttributeValueParser attributeValueParser,
        NcdfDefinition definition,
        String filterExpr
    ) {
        this.exprParser = exprParser;
        this.translate = translate;
        this.conn = conn;
        this.schema = schema;
        this.createWritable = createWritable;
        this.attributeValueParser = attributeValueParser;
        this.definition = definition;
        this.filterExpr = filterExpr;

        fetchSize = 10000;
        outputFormatter = null;
        featureInstancesRS = null;
    }


    private String getVirtualDataTable() {
        return definition.getDataSource().getVirtualDataTable();
    }

    private String getVirtualInstanceTable() {
        return definition.getDataSource().getVirtualInstanceTable();
    }

    public void prepare(IOutputFormatter outputFormatter) throws Exception
    {
        this.outputFormatter = outputFormatter;

        // do not quote search path!.
        PreparedStatement pathStmt = conn.prepareStatement("set search_path=" + schema + ", public");
        pathStmt.execute();
        pathStmt.close();

        // Batch results set
        conn.setAutoCommit(false);

        IExpression selectionExpr = exprParser.parseExpression(filterExpr);
        selectionClause = translate.process(selectionExpr);

        // if we combine both tables, then it's actually simpler, since don't need to process twice
        // or discriminate about which attributes come from which tables.
        // And there's no optimisation penalty since both the initial and instance queries have to hit the big data table
        String instanceQuery =
            "select distinct data.instance_id"
            + " from (" + getVirtualDataTable() + ") as data"
            + " left join (" + getVirtualInstanceTable() + ") instance"
            + " on instance.id = data.instance_id"
            + " where " + selectionClause
            + ";";


        PreparedStatement featuresStmt = conn.prepareStatement(instanceQuery);
        featuresStmt.setFetchSize(fetchSize);

        // change name featureInstancesRSToProcess ?
        featureInstancesRS = featuresStmt.executeQuery();
    }


    public boolean writeNext() throws Exception
    {
        if (!featureInstancesRS.next()) {
            logger.info("no more instances");
            if(outputFormatter != null) {
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
            "select *"
            + " from (" + getVirtualDataTable() + ") as data"
            + " left join (" + getVirtualInstanceTable() + ") instance"
            + " on instance.id = data.instance_id"
            + " where " + selectionClause
            + " and data.instance_id = " + Long.toString(instanceId)
            + " order by " + orderClause
            + ";";

        logger.debug("instanceId " + instanceId + ", " + query);

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
                value = evaluateSql(instanceId, selectionClause, orderClause, attribute.getSql());
            }
            else {
                throw new NcdfGeneratorException("No value defined for global attribute '" + name + "'");
            }

            if (value == null) {
                logger.warn("Null attribute value '" + name + "'");
            }
            else if (value instanceof Number) {
                writer.addGlobalAttribute(name, (Number)value);
            }
            else if (value instanceof String) {
                writer.addGlobalAttribute(name, (String)value);
            }
            else if (value instanceof Array) {
                writer.addGlobalAttribute(name, (Array)value);
            }
            else {
                throw new NcdfGeneratorException("Unrecognized attribute type '" + value.getClass().getName() + "'");
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

        // get filename
        Object filename = evaluateSql(instanceId, selectionClause, orderClause, definition.getFilenameTemplate().getSql());

        // this is awful...
        // format the file into the output stream
        InputStream is = createWritable.getStream();
        outputFormatter.write((String)filename, is);
        is.close();

        return true ;
    }


    private Object evaluateSql(long instanceId, String selectionClause, String orderClause, String sql) throws Exception {
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
            + " left join (" + getVirtualInstanceTable() + ") instance"
            + " on instance.id = data.instance_id"
            + " where " + selectionClause
            + " and data.instance_id = " + Long.toString(instanceId)
            + " order by " + orderClause
            + " ) as data"
        );

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
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setFetchSize(fetchSize);
        ResultSet rs = stmt.executeQuery();

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
        ArrayList<IAddValue>[] processing = (ArrayList<IAddValue>[])new ArrayList[numColumns + 1];

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

