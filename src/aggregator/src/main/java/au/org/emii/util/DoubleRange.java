package au.org.emii.util;

public class DoubleRange {

    public final Double start;
    public final Double end;

    public DoubleRange(String start, String end) {
        this.start = Double.parseDouble(start);
        this.end = Double.parseDouble(end);

    }
}