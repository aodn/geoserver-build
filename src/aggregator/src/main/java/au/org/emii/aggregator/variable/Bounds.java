package au.org.emii.aggregator.variable;

public class Bounds {
    private final double min;
    private final double max;

    public Bounds(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}

