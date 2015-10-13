package au.org.emii.ncdfgenerator;

import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Map;

class ByteValueEncoder implements IValueEncoder {
    private byte fill;

    ByteValueEncoder() {
        this.fill = 0x0;
    }

    public DataType targetType() {
        return DataType.BYTE;
    }

    public void prepare(Map<String, Object> attributes) throws NcdfGeneratorException {
        try {
            fill = (Byte)attributes.get("_FillValue");
        }
        catch (Exception e) {
            throw new NcdfGeneratorException("Expected _FillValue attribute to be Byte type");
        }
    }

    public void encode(Array array, int index, Object value) throws NcdfGeneratorException {
        if (value == null) {
            array.setByte(index, fill);
        }
        else if (value instanceof Byte) {
            array.setByte(index, (Byte)value);
        }
        else if (value instanceof String && ((String)value).length() == 1) {
            // interpret one char string literals using ascii value
            String s = (String)value;
            int ch = s.getBytes()[0] - '0';
            array.setByte(index, (byte)ch);
        }
        else {
            throw new NcdfGeneratorException("Failed to convert type to byte");
        }
    }
}
