package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;
import java.io.InputStream;

interface ICreateWriter {
    // open a netcdffile ready for writing
    NetcdfFileWriter create() throws IOException;

    // get the stream of the netcdf
    InputStream getStream() throws IOException;
}


