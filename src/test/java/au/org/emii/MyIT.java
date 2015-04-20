
package au.org.emii.ncdfgenerator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import java.io.InputStream ;
import java.io.OutputStream ;
import java.io.FileOutputStream ;


// import ucar.nc2.NetcdfFileWriteable;

import java.sql.*;


public class MyIT {

	@Before
	public void mergeIT() {

		// setup db conn once?
	}

	public static Connection getConn() throws Exception
	{
		String url = "jdbc:postgresql://115.146.94.132/harvest";   // nectar instance, needs to move to test resources configuration
		Properties props = new Properties();
		props.setProperty("user","meteo");
		props.setProperty("password","meteo");


		props.setProperty("ssl","true");
		props.setProperty("sslfactory","org.postgresql.ssl.NonValidatingFactory");
		props.setProperty("driver","org.postgresql.Driver" );

		return DriverManager.getConnection(url, props);
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

		System.out.println( "**** anmn timeseries ****" );
		InputStream config = getClass().getResourceAsStream("/anmn_timeseries.xml");

		String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";
		// String cql = " TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";


		NcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());
		streamData( encoder );
		System.out.println( "finished test" );
	}

	@Test
	public void anmn_nrs_ctd_profiles_IT() throws Exception {

		System.out.println( "**** anmn_nrs_ctd_profiles **** " );
		// exception handling needs to be improved a lot...

		InputStream config = getClass().getResourceAsStream("/anmn_nrs_ctd_profiles.xml");
		String cql = "TIME < '2013-6-29T00:40:01Z' ";
		NcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());

		streamData( encoder );
		System.out.println( "finished test" );
	}

	@Test
	public void soop_sst_trajectory_IT() throws Exception {

		System.out.println( "**** sst trajectory ****" );
		InputStream config = getClass().getResourceAsStream("/soop_sst_trajectory.xml");

		String cql = "TIME >= '2013-6-27T00:35:01Z' AND TIME <= '2013-6-29T00:40:01Z' ";
		NcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());

		streamData( encoder );
		System.out.println( "finished test" );
	}




	// test zip streaming of data...
	@Test
	public void anmn_timeseries2_IT() throws Exception 
	{
		System.out.println( "**** anmn timeseries ****" );
		InputStream config = getClass().getResourceAsStream("/anmn_timeseries.xml");

		String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";
		// String cql = " TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

		INcdfEncoder encoder = new NcdfEncoderBuilder().create( config, cql, getConn());
		ZipCreator zipCreator = new ZipCreator( encoder); 

		OutputStream os = new FileOutputStream( "./myoutput2.zip" );
		zipCreator.doStreaming( os );  
		os.close();

		System.out.println( "finished test" );
	}


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

}

