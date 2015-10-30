package au.org.emii.ncdfgenerator;

import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Map;

public class DoubleValueEncoder implements IValueEncoder {
    private double fill;
    private boolean haveFill;

    DoubleValueEncoder() {
        this.fill = 1234.;
        this.haveFill = false;
    }

    public final DataType targetType() {
        return DataType.DOUBLE;
    }

    public final void prepare(Map<String, Object> attributes) throws NcdfGeneratorException {
        if (attributes.get("_FillValue") != null) {
            try {
                fill = (Double)attributes.get("_FillValue");
            }
            catch (Exception e) {
                throw new NcdfGeneratorException("Expected _FillValue attribute to be Double type");
            }
            haveFill = true;
        }
    }

    public final void encode(Array array, int index, Object value) throws NcdfGeneratorException {
        if (value == null) {
            if (haveFill) {
                array.setDouble(index, fill);
            }
            else {
                throw new NcdfGeneratorException("Missing value and no fill attribute defined");
            }
        }
        else if (value instanceof Float) {
            array.setDouble(index, (double)(Float)value);
        }
        else if (value instanceof Double) {
            array.setDouble(index, (Double)value);
        }
        else {
            throw new NcdfGeneratorException("Failed to coerce type '" + value.getClass() + "' to double");
        }
    }
}

