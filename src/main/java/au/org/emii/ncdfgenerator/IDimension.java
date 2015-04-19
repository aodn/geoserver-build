
package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Dimension;


interface IDimension extends IAddValue
{
	public void define( NetcdfFileWriteable writer) ;
	public Dimension getDimension( ) ; // horrible to expose this...
										// can't the caller create the dimension?
	public int getLength();
	public void addValueToBuffer( Object value );
	public String getName();
}
