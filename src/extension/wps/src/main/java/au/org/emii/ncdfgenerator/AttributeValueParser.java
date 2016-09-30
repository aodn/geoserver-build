package au.org.emii.ncdfgenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.ArrayList;
import java.util.List;

class AttributeValueParser implements IAttributeValueParser {
    // follows ncdf string conventions

    private static final Logger logger = LoggerFactory.getLogger(AttributeValueParser.class);

    private int skipWhite(String s, int pos) {
        while (Character.isSpaceChar(peekChar(s, pos))) {
            ++pos;
        }
        return pos;
    }

    private int peekChar(String s, int pos) {
        if (pos < s.length()) {
            return s.charAt(pos);
        }
        return -1;
    }

    public AttributeValue parse(String s) throws NcdfGeneratorException {
        int pos = 0;

        AttributeValue a = parseAttributes(s, pos);
        if (a != null) {
            // ensure we got to the end, with nothing trailing
            pos = a.getPosition();
            pos = skipWhite(s, pos);
            if (pos == s.length()) {
                return a;
            }
        }

        throw new NcdfGeneratorException("Couldn't parse attribute value '" + s + "'");
    }

    protected AttributeValue parseAttributes(String s, int pos) throws NcdfGeneratorException {
        // try to parse as an attribute array, fallback to creating a scalar

        List<AttributeValue> items = new ArrayList<AttributeValue>();

        AttributeValue a = parseAttributeValue1(s, pos);
        if (a == null) {
            return null;
        }

        items.add(a);
        pos = a.getPosition();
        pos = skipWhite(s, pos);

        while (peekChar(s, pos) == ',') {
            ++pos;
            pos = skipWhite(s, pos);
            a = parseAttributeValue1(s, pos);
            if (a == null) {
                return null;
            }
            items.add(a);
            pos = a.getPosition();
            pos = skipWhite(s, pos);
        }

        if (items.size() == 1) {
            return items.get(0);
        }
        else if (items.size() > 1) {
            // handle single dimension only at this point...
            int[] shape = {items.size()};

            // assume type according to the first value
            Object first = items.get(0).getValue();
            Array ar = null;

            if (first instanceof Byte) {
                ar = Array.factory(DataType.BYTE, shape);
                int i = 0;
                for (AttributeValue e : items) {
                    ar.setByte(i++, (Byte)e.getValue());
                }
            }
            else if (first instanceof Character) {
                ar = Array.factory(DataType.CHAR, shape);
                int i = 0;
                for (AttributeValue e : items) {
                    ar.setChar(i++, (Character)e.getValue());
                }
            }
            else if (first instanceof Integer) {
                ar = Array.factory(DataType.INT, shape);
                int i = 0;
                for (AttributeValue e : items) {
                    ar.setInt(i++, (Integer)e.getValue());
                }
                // TODO long
            }
            else if (first instanceof Float) {
                ar = Array.factory(DataType.FLOAT, shape);
                int i = 0;
                for (AttributeValue e : items) {
                    ar.setFloat(i++, (Float)e.getValue());
                }
            }
            else if (first instanceof Double) {
                ar = Array.factory(DataType.DOUBLE, shape);
                int i = 0;
                for (AttributeValue e : items) {
                    ar.setDouble(i++, (Double)e.getValue());
                }
                // don't think strings are supported by api?
            }
            else {
                // more a runtime exception
                throw new NcdfGeneratorException("Unknonwn array value type '" + first.getClass().getName() + "'");
            }
            return new AttributeValue(pos, ar);
        }
        else {
            return null;
        }
    }

    protected AttributeValue parseAttributeValue1(String s, int pos) {
        AttributeValue a = parseFloat(s, pos);
        if (a != null) {
            return a;
        }

        a = parseInteger(s, pos);
        if (a != null) {
            return a;
        }

        a = parseString(s, pos);
        if (a != null) {
            return a;
        }
        return null;
    }

    protected AttributeValue parseFloat(String s, int pos) {
        int pos2 = pos;

        if (peekChar(s, pos2) == '-') {
            ++pos2;
        }

        boolean gotDot = false;
        while (Character.isDigit(peekChar(s, pos2))
            || peekChar(s, pos2) == '.'
            ) {
            if (peekChar(s, pos2) == '.') {
                gotDot = true;
            }
            ++pos2;
        }

        if (pos == pos2 || !gotDot) {
            return null;
        }

        if (peekChar(s, pos2) == 'f') {
            float value = Float.parseFloat(s.substring(pos, pos2));
            ++pos2;
            return new AttributeValue(pos2, value);
        }
        else {
            double value = Double.parseDouble(s.substring(pos, pos2));
            return new AttributeValue(pos2, value);
        }
    }

    protected AttributeValue parseInteger(String s, int pos) {
        int pos2 = pos;

        if (peekChar(s, pos2) == '-') {
            ++pos2;
        }

        while (Character.isDigit(peekChar(s, pos2))) {
            ++pos2;
        }

        if (pos == pos2) {
            return null;
        }

        int value = Integer.parseInt(s.substring(pos, pos2));

        if (peekChar(s, pos2) == 'b') {
            ++pos2;
            return new AttributeValue(pos2, (byte)value);
        }
        else {
            return new AttributeValue(pos2, value);
        }
    }

    protected AttributeValue parseString(String s, int pos) {
        // TODO ignore escaping for the moment
        // support single quoted strings too, since xml escaping double quotes with &quot; is horrid
        int pos2 = pos;
        if (peekChar(s, pos2) != '"' && peekChar(s, pos2) != '\'') {
            return null;
        }

        int closeChar = peekChar(s, pos2);
        ++pos2;
        while (peekChar(s, pos2) != closeChar) {
            ++pos2;
            if (pos2 == s.length()) {
                // Catch case of non terminating netCdf attribute
                logger.debug(String.format("Looks like string attribute not properly terminated %s", s));
                return null;
            }
        }
        ++pos2;
        String value = s.substring(pos + 1, pos2 - 1);
        return new AttributeValue(pos2, value);
    }
}


