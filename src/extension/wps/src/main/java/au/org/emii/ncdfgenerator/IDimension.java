package au.org.emii.ncdfgenerator;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;

interface IDimension extends IAddValue {
    void define(NetcdfFileWriter writer);

    Dimension getDimension();  // horrible to expose this...

    // can't the caller create the dimension?
    int getLength();

    void addValueToBuffer(Object value);

    String getName();

    boolean isUnlimited();
}
