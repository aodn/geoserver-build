
package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriteable;


public interface IVariableEncoder extends IAddValue
{
	public void define( NetcdfFileWriteable writer ) throws NcdfGeneratorException; 
	public void finish( NetcdfFileWriteable writer) throws Exception ; // TODO should be NcdfGeneratorException
	public void addValueToBuffer( Object value );
	public String getName();
}


