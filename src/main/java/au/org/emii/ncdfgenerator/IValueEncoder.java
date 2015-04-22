
package au.org.emii.ncdfgenerator;

import java.util.Map;
import ucar.ma2.Array;
import ucar.ma2.DataType;


public interface IValueEncoder
{
	// Netcdf value encoder from java/sql types

	public void prepare( Map<String, Object> attributes ) throws NcdfGeneratorException;
	public void encode( Array array, int ima, Object value ) throws NcdfGeneratorException;
	public DataType targetType();
}


