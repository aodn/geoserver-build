package au.org.emii.ncdfgenerator;

import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimestampValueEncoder implements IValueEncoder {
    // Always double type, for the moment

    private long epoch;  // in seconds
    private String unit; // seconds, days
    private double fill;
    private boolean haveFill;

    TimestampValueEncoder() {
        // all the date attribute parsing slows the code a lot so calculate once at init .
        this.epoch = 0;
        this.unit = null;
        this.fill = 1234;
        this.haveFill = false;
    }

    public final DataType targetType() {
        return DataType.DOUBLE;
    }

    public final void prepare(Map<String, Object> attributes) throws NcdfGeneratorException {
        if (attributes.get("units") == null) {
            throw new NcdfGeneratorException("Missing 'units' attribute required for time coding");
        }

        String units = (String)attributes.get("units");

        Matcher m = Pattern.compile("([a-zA-Z]*)[ ]*since[ ]*(.*)").matcher(units);
        if (!m.find()) {
            throw new NcdfGeneratorException("Couldn't parse attribute date");
        }
        unit = m.group(1);
        String epochString = m.group(2);
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            Date ts = df.parse(epochString);
            epoch = (Long)ts.getTime() / 1000;
        }
        catch (Exception e) {
            throw new NcdfGeneratorException("Couldn't extract timestamp '" + epochString + "' " + e.getMessage());
        }

        if (attributes.get("_FillValue") != null) {
            fill = (Double)attributes.get("_FillValue");
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
        else if (value instanceof java.sql.Timestamp) {
            double seconds = ((java.sql.Timestamp)value).getTime() / 1000.0d;
            double val = seconds - epoch;
            if (unit.equals("days")) {
                val /= 86400.0d;
            }
            else if (unit.equals("minutes")) {
                val /= 60.0d;
            }
            else if (unit.equals("seconds")) {
            }
            else {
                throw new NcdfGeneratorException("Unrecognized time unit " + unit);
            }

            array.setDouble(index, val);
        }
        else {
            throw new NcdfGeneratorException("Not a timestamp");
        }
    }
}
