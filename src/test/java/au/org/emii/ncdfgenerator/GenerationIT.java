
package au.org.emii.ncdfgenerator;

import au.org.emii.ncdfgenerator.cql.CQLException;
import org.postgresql.util.PSQLException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.System;

import java.sql.*;

import au.org.emii.ncdfgenerator.IOutputFormatter;

import au.org.emii.ncdfgenerator.cql.ExprParser;
import au.org.emii.ncdfgenerator.cql.IExprParser;
import au.org.emii.ncdfgenerator.cql.IDialectTranslate;
import au.org.emii.ncdfgenerator.cql.PGDialectTranslate;
import au.org.emii.ncdfgenerator.cql.PGDialectTranslate;
import au.org.emii.ncdfgenerator.IOutputFormatter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;


class MockFormatter implements IOutputFormatter
{
    public final void prepare(OutputStream os) {
    }

    public final void write(String filename, InputStream is) {
    }

    public final void finish() {
    }
}

public class GenerationIT {

    static final String TMPDIR = "./tmp";

    @Before
    public void setup() {

        // TODO factor this name into var
        new File(TMPDIR).mkdirs();
    }

    private NcdfEncoder getEncoder( InputStream config, String filterExpr, Connection conn ) throws Exception {

        // we can't use the builder for this, becuase config is a stream...

        IExprParser parser = new ExprParser();
        IDialectTranslate translate = new PGDialectTranslate();
        ICreateWritable createWritable = new CreateWritable(TMPDIR); // TODO factor
        IAttributeValueParser attributeValueParser = new AttributeValueParser();

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
        Node node = document.getFirstChild();
        NcdfDefinition definition = new NcdfDefinitionXMLParser().parse(node);

        // better responsibility separation
        // change name from outputGenerator to outputFormatter....
        IOutputFormatter outputGenerator = new MockFormatter();

        return new NcdfEncoder(parser, translate, conn, createWritable, attributeValueParser, definition, filterExpr, outputGenerator, System.out);

    }

    public static Connection getConn() throws Exception {
        Map<String, String> env = System.getenv();

        String opts [] = { "POSTGRES_USER", "POSTGRES_PASS", "POSTGRES_JDBC_URL" } ;
        for(String opt : opts) {
            if(env.get(opt) == null)
                throw new Exception("Environment var '" + opt + "' not set");
        }

        Properties props = new Properties();
        props.setProperty("user", env.get("POSTGRES_USER"));
        props.setProperty("password", env.get("POSTGRES_PASS"));
        props.setProperty("ssl","true");
        props.setProperty("sslfactory","org.postgresql.ssl.NonValidatingFactory");
        props.setProperty("driver","org.postgresql.Driver");

        return DriverManager.getConnection(env.get("POSTGRES_JDBC_URL"), props);
    }


    private void doTypenameQuery(InputStream config, String cql) throws Exception {

        NcdfEncoder encoder = getEncoder(config, cql, getConn());
        encoder.write();
    }


    private void doSimpleQuery(String cql) throws Exception {
        InputStream config = getClass().getResourceAsStream("/anmn_ts.xml");
        NcdfEncoder encoder = getEncoder(config, cql, getConn());
        encoder.write();
    }


    @Test
    public void anmn_nrs_ctd_profiles_IT() throws Exception {
        String cql = "TIME < '2013-6-29T00:40:01Z' ";
        String layerConfigDir = "./src/test/resources/"; // TODO URL url = getClass().getResource("/")  ; url.toString()...
        String tmpCreationDir = TMPDIR;
        NcdfGenerator generator = new NcdfGenerator(layerConfigDir, tmpCreationDir);
        OutputStream os = new FileOutputStream(TMPDIR + "/output.zip");
        generator.write("anmn_nrs_ctd_profiles", cql, getConn(), os);
    }

    @Test
    public void soop_sst_trajectory_IT() throws Exception {

        InputStream config = getClass().getResourceAsStream("/soop_sst_trajectory.xml");
        String cql = "TIME >= '2013-6-27T00:35:01Z' AND TIME <= '2013-6-29T00:40:01Z' ";
        doTypenameQuery(config, cql);
    }

    // test zip streaming using NcdfGenerator
    @Test
    public void ncdfGenerator_IT() throws Exception {
        String layerConfigDir = "./src/test/resources/"; // TODO URL url = getClass().getResource("/")  ; url.toString()...
        String tmpCreationDir = TMPDIR;
        NcdfGenerator generator = new NcdfGenerator(layerConfigDir, tmpCreationDir);

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        OutputStream os = new FileOutputStream(TMPDIR + "/output.zip");
        generator.write("anmn_ts", cql, getConn(), os);
    }


    @Test
    public void cql_with_valid_spatial_temporal_subset() throws Exception {
        // A
        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        doSimpleQuery(cql);
    }

    /* TODO: catch empty downloads */
    @Test
    public void cql_with_no_data_in_spatial_subset() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((163.7841796875 -15.9970703125,163.7841796875 -3.0771484375,173.8037109375 -3.077148437499999,173.8037109375 -15.9970703125,163.7841796875 -15.9970703125))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        doSimpleQuery(cql); // TODO check empty or not created etc...
    }

    /* TODO: catch empty downloads */
    @Test
    public void cql_with_temporal_extent_out_of_allowed_range() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((113.33 -33.09,113.33 -30.98,117.11 -30.98,117.11 -33.09,113.33 -33.09))) AND TIME >= '1949-01-01T23:00:00Z' AND TIME <= '1951-01-01T00:00:00Z'";

        doSimpleQuery(cql);  // TODO check empty or not created etc...
    }

    /* TODO: handle longitude outside of range earlier than PSQL exception */
    @Test(expected = PSQLException.class)
    public void cql_longitude_outside_allowed_range() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((182 -33.09,113.33 -30.98,117.11 -30.98,117.11 -33.09,113.33 -33.09))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        doSimpleQuery(cql);
    }

    /* TODO: handle longitude outside of range earlier than PSQL exception */
    @Test(expected = PSQLException.class)
    public void cql_latitude_outside_allowed_range() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((113.33 -95,113.33 -30.98,117.11 -30.98,117.11 -33.09,113.33 -33.09))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        doSimpleQuery(cql);
    }

    @Test
    public void cql_with_float_equality_valid() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' AND NOMINAL_DEPTH = 5000.50";
        doSimpleQuery(cql);
        // TODO.. check the data... that norminal depth = 5000 etc...
    }

    @Test(expected = CQLException.class)
    public void cql_with_float_equality_invalid() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' AND NOMINAL_DEPTH = 5000.";

        doSimpleQuery(cql);
    }


    /* TODO: Implement not parsing for floats and ints */
    @Test
    public void cql_with_float_not_statement_valid() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' AND NOMINAL_DEPTH <> 5000.50";
        doSimpleQuery(cql);
    }


    /* TODO: having problems parsing ints (or bytes) */
    @Test
    public void cql_with_int_equality_valid() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' AND TEMP_quality_control = 5";
        doSimpleQuery(cql);

        // TODO IMPORTANT !!! NOT WORKING...
    }
}

