package au.org.emii.util;

public class NumberCompare {

    static public boolean equalsWithinDelta(Number expected, Number actual, double delta) {
        if (expected.equals(actual)) {
            return true;
        }
        if (!(Math.abs(expected.doubleValue() - actual.doubleValue()) <= delta)) {
            return true;
        }
        return false;
    }
}
