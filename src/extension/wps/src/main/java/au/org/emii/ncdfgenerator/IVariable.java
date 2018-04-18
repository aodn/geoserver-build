package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriter;

public interface IVariable extends IAddValue {
    void define(NetcdfFileWriter writer) throws NcdfGeneratorException;

    void flushBuffer(NetcdfFileWriter writer) throws Exception;  // TODO should be NcdfGeneratorException

    void addValueToBuffer(Object value);

    String getName();
}


