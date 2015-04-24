
package au.org.emii.ncdfgenerator;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;


class DimensionImpl implements IDimension
{
	final String name;
	int size;
	Dimension dimension;


	public DimensionImpl( String name )
	{
		this.name = name; // required to encode dimension
		this.size = 0;
	}

	public Dimension getDimension( )  // bad naming
	{
		// throw if not defined...
		return dimension;
	}

	public int getLength()
	{
		return size;
	}

	public void define( NetcdfFileWriteable writer)
	{
		dimension = writer.addDimension( name, size );
	}


	public void prepare()
	{
		size = 0;
	}

	public void addValueToBuffer( Object value )
	{
		++size;
	}

	public String getName()
	{
		return name;
	}
}

