package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.io.InputStream;

interface ICreateWritable {
    // open a netcdffile ready for writing
    NetcdfFileWriteable create() throws IOException;

    // get the stream of the netcdf
    InputStream getStream() throws IOException;
}


