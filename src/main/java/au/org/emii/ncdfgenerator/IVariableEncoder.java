
package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriteable;


public interface IVariableEncoder extends IAddValue
{
	public void define( NetcdfFileWriteable writer ) ;
	public void finish( NetcdfFileWriteable writer) throws Exception ;
	public void addValueToBuffer( Object value );
	public String getName(); // change class name to IVariableEncoder and this to just getName()
}


