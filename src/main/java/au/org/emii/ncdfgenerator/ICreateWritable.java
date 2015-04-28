
package au.org.emii.ncdfgenerator;

import java.io.InputStream;
import ucar.nc2.NetcdfFileWriteable;
import java.io.IOException;

interface ICreateWritable {
    // open a netcdffile ready for writing
    NetcdfFileWriteable create() throws IOException;

    // get the stream of the netcdf
    InputStream getStream() throws IOException;
}


