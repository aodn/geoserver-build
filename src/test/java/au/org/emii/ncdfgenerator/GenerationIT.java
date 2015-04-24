
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


public class GenerationIT {

	@Before
	public void mergeIT() {

		// setup db conn once?
	}

	public static Connection getConn() throws Exception
	{
		Map<String, String> env = System.getenv();

		String opts [] = { "POSTGRES_USER", "POSTGRES_PASS", "POSTGRES_JDBC_URL" } ;
		for ( String opt : opts ) {
			if( env.get( opt) == null)
				throw new Exception( "Environment var '" + opt + "' not set" );
		}

		Properties props = new Properties();
		props.setProperty("user", env.get( "POSTGRES_USER" ) );
		props.setProperty("password", env.get( "POSTGRES_PASS" ) );
		props.setProperty("ssl","true");
		props.setProperty("sslfactory","org.postgresql.ssl.NonValidatingFactory");
		props.setProperty("driver","org.postgresql.Driver" );

		return DriverManager.getConnection( env.get( "POSTGRES_JDBC_URL" ), props);
	}

	private void streamData( INcdfEncoder encoder ) throws Exception {
		InputStream writer = null;
		do {
			// should try and get lots...
			writer = encoder.get();
		}
		while( writer != null );
	}


	@Test
	public void anmn_timeseries_IT() throws Exception {

		InputStream config = getClass().getResourceAsStream("/anmn_timeseries.xml");

		String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

		NcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());
		streamData( encoder );
	}

	@Test
	public void anmn_nrs_ctd_profiles_IT() throws Exception {

		// exception handling needs to be improved a lot...

		InputStream config = getClass().getResourceAsStream("/anmn_nrs_ctd_profiles.xml");
		String cql = "TIME < '2013-6-29T00:40:01Z' ";
		NcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());

		streamData( encoder );
	}

	@Test
	public void soop_sst_trajectory_IT() throws Exception {

		InputStream config = getClass().getResourceAsStream("/soop_sst_trajectory.xml");

		String cql = "TIME >= '2013-6-27T00:35:01Z' AND TIME <= '2013-6-29T00:40:01Z' ";
		NcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());

		streamData( encoder );
	}


	// test zip streaming of data using builder...
	@Test
	public void anmn_timeseries2_IT() throws Exception
	{
		InputStream config = getClass().getResourceAsStream("/anmn_timeseries.xml");

		String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

		INcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());
		ZipCreator zipCreator = new ZipCreator( encoder);

		OutputStream os = new FileOutputStream( "./myoutput2.zip" );
		zipCreator.doStreaming( os );
		os.close();
	}


	// test zip streaming using NcdfGenerator
	@Test
	public void ncdfGenerator_IT() throws Exception
	{
		String layerConfigDir = "./src/test/resources/"; // TODO URL url = getClass().getResource("/")  ; url.toString()...
		String tmpCreationDir = "./tmp";
		NcdfGenerator generator = new NcdfGenerator( layerConfigDir, tmpCreationDir );

		String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

		OutputStream os = new FileOutputStream( "./tmp/output.zip" );

		generator.write( "anmn_timeseries", cql, getConn(), os );
	}



	@Test
	public void anmn_timeseries_gg_IT() throws Exception {

		InputStream config = getClass().getResourceAsStream("/anmn_timeseries_gg.xml");

		String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";


		NcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());
		streamData( encoder );
	}

}

