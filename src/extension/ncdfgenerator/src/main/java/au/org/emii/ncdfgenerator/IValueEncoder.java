package au.org.emii.ncdfgenerator;

import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Map;

public interface IValueEncoder {
    // Netcdf value encoder from java/sql types
    void prepare(Map<String, Object> attributes) throws NcdfGeneratorException;

    void encode(Array array, int index, Object value) throws NcdfGeneratorException;

    DataType targetType();
}

