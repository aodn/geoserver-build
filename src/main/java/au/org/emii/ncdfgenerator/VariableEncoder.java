
package au.org.emii.ncdfgenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ucar.nc2.NetcdfFileWriteable;
import ucar.ma2.Array;
import ucar.nc2.Dimension;


class VariableEncoder implements IVariableEncoder
{
	final String				variableName;
	final IValueEncoder			encodeValue;
	final Map<String, String>	attributes;
	final ArrayList<IDimension>	dimensions; // change name childDimensions
	final ArrayList<Object>		buffer;


	public VariableEncoder(
		String variableName,
		ArrayList< IDimension> dimensions,
		IValueEncoder encodeValue,
		Map<String, String> attributes
	) {
		this.variableName = variableName;
		this.encodeValue = encodeValue;
		this.attributes = attributes;
		this.dimensions = dimensions;
		this.buffer = new ArrayList<Object>( );
	}


	/*	we can also record the table, or index of table here if we want
			to incorporate into the strategy.
		eg. we can compre with xml to decide what to do.
	*/
	public void addValueToBuffer( Object value )
	{
		// perhaps delegate to strategy...
		buffer.add( value );
	}

	public void define( NetcdfFileWriteable writer )
	{
		// write dims and attributes

		// make sure children are defined already
		List<Dimension> d = new ArrayList<Dimension>();
		for( IDimension dimension: dimensions)
		{
			d.add( dimension.getDimension() );
		}

		writer.addVariable(variableName, encodeValue.targetType(), d );

		for( Map.Entry< String, String> entry : attributes.entrySet()) {
			writer.addVariableAttribute( variableName, entry.getKey(), entry.getValue()/*.toString()*/ );
		}
	}


	public void writeValues( ArrayList<IDimension> dims, int dimIndex, int acc, Array A  )
		throws NcdfGeneratorException
	{
		if( dimIndex < dims.size() )
		{
			Dimension dim = dims.get( dimIndex ).getDimension();
			for( int i = 0; i < dim.getLength(); i++ )
			{
				writeValues( dims, dimIndex + 1, acc + i, A );
			}
		}
		else
		{
			encodeValue.encode( A, acc, buffer.get( acc ) );
		}

	}

	private static int[] toIntArray( List<Integer> list)
	{
		// List.toArray() only supports java Boxed Integers...
		int[] ret = new int[list.size()];
		for(int i = 0;i < ret.length;i++)
			ret[i] = list.get(i);
		return ret;
	}

	public void finish( NetcdfFileWriteable writer) throws Exception
	{
		ArrayList< Integer> shape = new ArrayList< Integer>() ;
		for( IDimension dimension : dimensions ) {
			shape.add( dimension.getLength() );
		}

		Array A = Array.factory( encodeValue.targetType(), toIntArray(shape ) );

		encodeValue.prepare( attributes );

		writeValues( dimensions,  0, 0 , A );

		// int [] origin = new int[1];
		// writer.write(variableName, origin, A);
		writer.write(variableName, A);
	}


	public String getName()
	{
		return variableName;
	}
}

