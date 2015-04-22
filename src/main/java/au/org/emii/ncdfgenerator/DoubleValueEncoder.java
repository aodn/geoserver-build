
package au.org.emii.ncdfgenerator;

import java.util.Map;

import ucar.ma2.Array;
import ucar.ma2.DataType;


public class DoubleValueEncoder implements IValueEncoder
{
	double fill;
	boolean haveFill;

	DoubleValueEncoder()
	{
		this.fill = 1234.;
		this.haveFill = false;
	}

	public DataType targetType()
	{
		return DataType.DOUBLE;
	}

	public void prepare( Map<String, Object> attributes ) throws NcdfGeneratorException
	{
		if( attributes.get( "_FillValue" ) != null) 
		{
			try { 
				fill = (Double) attributes.get( "_FillValue" ); 
			} catch( Exception e ) {
				throw new NcdfGeneratorException( "Expected _FillValue attribute to be Double type");
			}
			haveFill = true;
		}
	}

	public void encode( Array array, int ima, Object value ) throws NcdfGeneratorException
	{
		if( value == null) {
			if( haveFill) 
				array.setDouble( ima, fill );
			else 
				throw new NcdfGeneratorException( "Missing value and no fill attribute defined" );
		}
		else if( value instanceof Float ) {
			array.setDouble( ima, (double)(Float) value);
		}
		else if( value instanceof Double ) {
			array.setDouble( ima, (double)value);
		}
		else {
			throw new NcdfGeneratorException( "Failed to coerce type '" + value.getClass() + "' to double" );
		}
	}
}

