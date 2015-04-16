
package au.org.emii.ncdfgenerator;

import java.io.IOException;

import ucar.nc2.NetcdfFileWriteable;


class CreateWritable implements ICreateWritable
{
	// an abstraction to support creating the ncf file
	// NetcdfFileWriteable is not an abstraction over a stream!. instead it insists on being a file...

	CreateWritable( String path )
	{
		this.path = path;
	}

	final String path;

	public NetcdfFileWriteable create() throws IOException
	{
		return NetcdfFileWriteable.createNew( path, false);
	}

	// TODO method to request as a byte stream and return?
	// public getByteStream () { }
}


