package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriteable;

public interface IVariable extends IAddValue {
    void define(NetcdfFileWriteable writer) throws NcdfGeneratorException;

    void finish(NetcdfFileWriteable writer) throws Exception;  // TODO should be NcdfGeneratorException

    void addValueToBuffer(Object value);

    String getName();
}


