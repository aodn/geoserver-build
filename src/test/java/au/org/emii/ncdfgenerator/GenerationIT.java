
package au.org.emii.ncdfgenerator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

// import java.io.Exception;
import java.io.InputStream ;
import java.io.OutputStream ;
import java.io.FileOutputStream ;

import java.lang.System;
import java.util.Map;

// import ucar.nc2.NetcdfFileWriteable;

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

    @Before
    public void before() {
    }

    private NcdfEncoder getEncoder( InputStream config, String filterExpr, Connection conn ) throws Exception {

        // we can't use the builder for this, becuase config is a stream...

        IExprParser parser = new ExprParser();
        IDialectTranslate translate = new PGDialectTranslate();
        ICreateWritable createWritable = new CreateWritable( "./tmp" );
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


    @Test
    public void anmn_timeseries_IT() throws Exception {

        InputStream config = getClass().getResourceAsStream("/anmn_timeseries.xml");
        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";
        NcdfEncoder encoder = getEncoder(config, cql, getConn());
        encoder.write();
    }

    @Test
    public void anmn_nrs_ctd_profiles_IT() throws Exception {

        // exception handling needs to be improved a lot...
        InputStream config = getClass().getResourceAsStream("/anmn_nrs_ctd_profiles.xml");
        String cql = "TIME < '2013-6-29T00:40:01Z' ";
        NcdfEncoder encoder = getEncoder(config, cql, getConn());
        encoder.write();
    }

    @Test
    public void soop_sst_trajectory_IT() throws Exception {

        InputStream config = getClass().getResourceAsStream("/soop_sst_trajectory.xml");

        String cql = "TIME >= '2013-6-27T00:35:01Z' AND TIME <= '2013-6-29T00:40:01Z' ";
        NcdfEncoder encoder = getEncoder(config, cql, getConn());
        encoder.write();
    }


    // test zip streaming of data using builder...
    @Test
    public void anmn_timeseries2_IT() throws Exception {
        InputStream config = getClass().getResourceAsStream("/anmn_timeseries.xml");

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";
        NcdfEncoder encoder = getEncoder( config, cql, getConn() );
        encoder.write();
    }


    // test zip streaming using NcdfGenerator
    @Test
    public void ncdfGenerator_IT() throws Exception {
        String layerConfigDir = "./src/test/resources/"; // TODO URL url = getClass().getResource("/")  ; url.toString()...
        String tmpCreationDir = "./tmp";
        NcdfGenerator generator = new NcdfGenerator(layerConfigDir, tmpCreationDir);

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        OutputStream os = new FileOutputStream("./tmp/output.zip");
        generator.write("anmn_timeseries", cql, getConn(), os);
    }


    @Test
    public void anmn_timeseries_gg_IT() throws Exception {
        String layerConfigDir = "./src/test/resources/"; // TODO URL url = getClass().getResource("/")  ; url.toString()...
        String tmpCreationDir = "./tmp";
        NcdfGenerator generator = new NcdfGenerator(layerConfigDir, tmpCreationDir);

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        OutputStream os = new FileOutputStream("./tmp/output.zip");
        generator.write("anmn_timeseries_gg", cql, getConn(), os);
    }
}

