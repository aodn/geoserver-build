package au.org.emii.aggregator.variable.datatype;

/**
 * Numeric DataType helper interface
 */
public interface NumericType {
    Number valueOf(Number number);

    boolean isDefaultFillValue(Number value);

    Number defaultFillValue();
}
