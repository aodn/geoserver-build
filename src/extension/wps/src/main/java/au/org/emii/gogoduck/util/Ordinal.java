package au.org.emii.gogoduck.util;

/**
 * Ordinal utility functions
 */
public class Ordinal {
    public static String suffix(int ordinal) {
        int j = ordinal % 10;
        int k = ordinal % 100;
        if (j == 1 && k != 11) {
            return "st";
        }
        if (j == 2 && k != 12) {
            return "nd";
        }
        if (j == 3 && k != 13) {
            return "rd";
        }
        return "th";
    }

}
