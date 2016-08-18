package au.org.emii.ncdfgenerator;

import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Map;

class CharValueEncoder implements IValueEncoder {
    private char fill;
    private boolean haveFill;

    CharValueEncoder() {
        this.fill = '\0';
        this.haveFill = false;
    }

    public DataType targetType() {
        return DataType.CHAR;
    }

    public final void prepare(Map<String, Object> attributes) throws NcdfGeneratorException {

        if (attributes.get("_FillValue") != null) {
            try {
                String s = (String)attributes.get("_FillValue");
                // Allow null values (\0). If the above statement didn't fail,
                // it means we had either a '' or '\0' string with length zero
                // so simply allow haveFill to become true and this.fill will
                // be set to '\0', otherwise, use first character of string
                if (!s.isEmpty()) {
                    fill = s.charAt(0);
                }
            }
            catch (Exception e) {
                throw new NcdfGeneratorException("Expected _FillValue attribute to be Char type");
            }
            haveFill = true;
        }
    }

    public void encode(Array array, int index, Object value) throws NcdfGeneratorException {
        if (value == null) {

            if (haveFill) {
                array.setChar(index, fill);
            }
            else {
                throw new NcdfGeneratorException("Missing value and no fill attribute defined");
            }
        }
        else if (value instanceof Character) {
            array.setChar(index, (Character)value);
        }
        else if (value instanceof String && ((String)value).length() == 1) {
            // interpret one char string literals using ascii value
            String s = (String)value;
            char ch = s.charAt(0);
            array.setChar(index, ch);
        }
        else {
            throw new NcdfGeneratorException("Failed to convert type to char");
        }
    }
}
