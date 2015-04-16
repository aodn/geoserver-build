
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
	// need a date unit...
	long epoch;  // in seconds
	String unit; // seconds, days
	float fill;

	TimestampValueEncoder()
	{
		// all the date attribute parsing slows the code a lot so calculate once at init .
		this.epoch = 0;
		this.unit = null;
		this.fill = 1234;
	}

	public DataType targetType()
	{
		return DataType.FLOAT;
	}

	public void prepare( Map<String, String> attributes ) throws NcdfGeneratorException
	{
		Matcher m = Pattern.compile("([a-zA-Z]*)[ ]*since[ ]*(.*)").matcher( attributes.get("units") );
		if(!m.find())
		{
			throw new NcdfGeneratorException( "couldn't parse attribute date");
		}
		unit = m.group(1);
		String epochString = m.group(2);
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
			Date ts = df.parse(epochString);
			epoch = (Long) ts.getTime() / 1000 ;
		} catch( Exception e )
		{
			throw new NcdfGeneratorException( "couldn't extract timestamp '" + epochString + "' " + e.getMessage()  );
		}

		fill = Float.valueOf( attributes.get( "_FillValue" )).floatValue();
	}

	public void encode( Array array, int ima,  Object value ) throws NcdfGeneratorException
	{

		if( value == null) {
			array.setFloat( ima, fill );
		}
		else if( value instanceof java.sql.Timestamp ) {
			long seconds =  ((java.sql.Timestamp)value).getTime() / 1000  ;
			long ret = seconds - epoch;
			if( unit.equals("days"))
				ret /= 86400;
			else if( unit.equals("minutes"))
				ret /= 1440;
			else if ( unit.equals("seconds"))
				;
			else
				throw new NcdfGeneratorException( "unrecognized time unit " + unit );

			array.setFloat( ima, (float) ret );
		}
		else {
			throw new NcdfGeneratorException( "Not a timestamp" );
		}
	}
}
