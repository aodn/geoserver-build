
package au.org.emii.ncdfgenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ucar.ma2.Array;
import ucar.ma2.DataType;


public class TimestampValueEncoder implements IValueEncoder
{
	// Always double type, for the moment

	long epoch;  // in seconds
	String unit; // seconds, days
	double fill;
	boolean haveFill;

	TimestampValueEncoder()
	{
		// all the date attribute parsing slows the code a lot so calculate once at init .
		this.epoch = 0;
		this.unit = null;
		this.fill = 1234;
		this.haveFill = false;
	}

	public DataType targetType()
	{
		return DataType.DOUBLE;
	}

	public void prepare( Map<String, Object> attributes ) throws NcdfGeneratorException
	{
		if( attributes.get("units") == null ) { 
			throw new NcdfGeneratorException( "Missing 'units' attribute required for time coding");
		}

		String units = (String) attributes.get("units"); 

		Matcher m = Pattern.compile("([a-zA-Z]*)[ ]*since[ ]*(.*)").matcher( units );
		if(!m.find())
		{
			throw new NcdfGeneratorException( "Couldn't parse attribute date");
		}
		unit = m.group(1);
		String epochString = m.group(2);
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
			Date ts = df.parse(epochString);
			epoch = (Long) ts.getTime() / 1000 ;
		} catch( Exception e )
		{
			throw new NcdfGeneratorException( "Couldn't extract timestamp '" + epochString + "' " + e.getMessage()  );
		}

		if( attributes.get( "_FillValue" ) != null) {
			fill = (Double) attributes.get( "_FillValue" );
			haveFill = true;
		}
	}

	public void encode( Array array, int ima,  Object value ) throws NcdfGeneratorException
	{

		if( value == null) {
			if( haveFill) 
				array.setDouble( ima, fill );
			else 
				throw new NcdfGeneratorException( "Missing value and no fill attribute defined" );
		}
		else if( value instanceof java.sql.Timestamp ) {
			long seconds =  ((java.sql.Timestamp)value).getTime() / 1000  ;
			long val = seconds - epoch;
			if( unit.equals("days"))
				val /= 86400;
			else if( unit.equals("minutes"))
				val /= 1440;
			else if ( unit.equals("seconds"))
				;
			else
				throw new NcdfGeneratorException( "Unrecognized time unit " + unit );

			array.setDouble( ima, (double) val );
		}
		else {
			throw new NcdfGeneratorException( "Not a timestamp" );
		}
	}
}
