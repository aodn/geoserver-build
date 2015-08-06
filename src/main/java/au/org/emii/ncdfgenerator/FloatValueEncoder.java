package au.org.emii.ncdfgenerator;

import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Map;

public class FloatValueEncoder implements IValueEncoder {
    private float fill;
    private boolean haveFill;

    FloatValueEncoder() {
        this.fill = 1234;
        this.haveFill = false;
    }

    public final DataType targetType() {
        return DataType.FLOAT;
    }

    public final void prepare(Map<String, Object> attributes) throws NcdfGeneratorException {
        if (attributes.get("_FillValue") != null) {
            try {
                fill = (Float)attributes.get("_FillValue");
            }
            catch (Exception e) {
                throw new NcdfGeneratorException("Expected _FillValue attribute to be Float type");
            }
            haveFill = true;
        }
    }

    public final void encode(Array array, int index, Object value) throws NcdfGeneratorException {
        if (value == null) {
            if (haveFill) {
                array.setFloat(index, fill);
            }
            else {
                throw new NcdfGeneratorException("Missing value and no fill attribute defined");
            }
        }
        else if (value instanceof Float) {
            array.setFloat(index, (Float)value);
        }
        else if (value instanceof Double) {
            array.setFloat(index, (float)(double)(Double)value);
        }
        else {
            throw new NcdfGeneratorException("Failed to coerce type '" + value.getClass() + "' to float");
        }
    }
}

