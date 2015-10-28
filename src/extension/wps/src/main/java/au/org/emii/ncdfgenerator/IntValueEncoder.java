package au.org.emii.ncdfgenerator;

import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Map;

public class IntValueEncoder implements IValueEncoder {
    // Int is 32bit in Netcdf

    private int fill;

    IntValueEncoder() {
        this.fill = 1234;
    }

    public final DataType targetType() {
        return DataType.INT;
    }

    public final void prepare(Map<String, Object> attributes) throws NcdfGeneratorException {
        try {
            fill = (Integer)attributes.get("_FillValue");
        }
        catch (Exception e) {
            throw new NcdfGeneratorException("Expected _FillValue attribute to be Int type");
        }
    }

    public final void encode(Array array, int index, Object value) throws NcdfGeneratorException {
        if (value == null) {
            array.setInt(index, fill);
        }
        else if (value instanceof Integer) {
            array.setInt(index, (Integer)value);
        }
        else if (value instanceof Long) {
            array.setInt(index, (int)(long)(Long)value);
        }
        else {
            throw new NcdfGeneratorException("Failed to coerce type '" + value.getClass() + "' to int");
        }
    }
}


