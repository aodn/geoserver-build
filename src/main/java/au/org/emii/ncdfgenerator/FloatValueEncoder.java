
package au.org.emii.ncdfgenerator;

import java.util.Map;

import ucar.ma2.Array;
import ucar.ma2.DataType;


public class FloatValueEncoder implements IValueEncoder
{
	float fill;

	FloatValueEncoder()
	{
		this.fill = 1234;
	}

	public DataType targetType()
	{
		return DataType.FLOAT;
	}

	public void prepare( Map<String, String> attributes )
	{
		fill = Float.valueOf( attributes.get( "_FillValue" )).floatValue();

	}

	public void encode( Array array, int ima, Object value ) throws NcdfGeneratorException
	{
		if( value == null) {
			array.setFloat( ima, fill );
		}
		else if( value instanceof Float ) {
			array.setFloat( ima, (Float) value);
		}
		else if( value instanceof Double ) {
			array.setFloat( ima, (float)(double)(Double) value);
		}
		else {
			throw new NcdfGeneratorException( "Failed to coerce type '" + value.getClass() + "' to float" );
		}
	}
}

