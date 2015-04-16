
package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriteable;
import java.io.IOException;

interface ICreateWritable
{
	public NetcdfFileWriteable create() throws IOException;
}


