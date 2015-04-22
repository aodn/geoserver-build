
package au.org.emii.ncdfgenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


import ucar.nc2.NetcdfFileWriteable;
import ucar.ma2.Array;
import ucar.nc2.Dimension;

import au.org.emii.ncdfgenerator.AttributeValue;


class VariableEncoder implements IVariableEncoder
{
	final String				variableName;
	final IValueEncoder			encodeValue;
	final Map<String, String>	attributes;
	final ArrayList<IDimension>	dimensions; // change name childDimensions
	final ArrayList<Object>		buffer;

	final IAttributeValueParser	attributeValueParser; 
	final Map< String, Object > convertedAttributes;

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

		this.attributeValueParser = new AttributeValueParser();  // TODO this class should not be responsible to instantiate 
		this.convertedAttributes = new HashMap< String, Object > ();
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

	public void define( NetcdfFileWriteable writer ) throws NcdfGeneratorException
	{
		// write dims and attributes

		// make sure children are defined already
		List<Dimension> d = new ArrayList<Dimension>();
		for( IDimension dimension: dimensions)
		{
			d.add( dimension.getDimension() );
		}

		writer.addVariable(variableName, encodeValue.targetType(), d );

		// decode the attribute values 
		for( Map.Entry< String, String> entry : attributes.entrySet()) {
			AttributeValue a = attributeValueParser.parse( entry.getValue() ); 
			convertedAttributes.put( entry.getKey(), a.value );
		}

		// encode the variable attributes 
		for( Map.Entry< String, Object> entry : convertedAttributes.entrySet()) {

			// https://www.unidata.ucar.edu/software/thredds/v4.3/netcdf-java/v4.2/javadoc/ucar/nc2/NetcdfFileWriteable.html
			Object value = entry.getValue(); 
			if( value instanceof Number ) {
				writer.addVariableAttribute( variableName, entry.getKey(), (Number) value );
			}  
			else if( value instanceof String ) {
				writer.addVariableAttribute( variableName, entry.getKey(), (String) value );
			}  
			else if( value instanceof Array ) {
				writer.addVariableAttribute( variableName, entry.getKey(), (Array) value );
			}  
			else {
				// TODO array
				throw new NcdfGeneratorException( "Unrecognized attribute type '" +  value.getClass().getName() + "'" ); 
			}
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


		encodeValue.prepare( convertedAttributes );

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

