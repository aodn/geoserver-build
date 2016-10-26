package au.org.emii.ncdfgenerator;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

class DimensionImpl implements IDimension {
    private final String name;
    private int length;
    private Dimension dimension;


    public DimensionImpl(String name) {
        this.name = name; // required to encode dimension
        this.length = 0;
    }

    public DimensionImpl(String name, int length) {
        this(name);
        this.length = length;
    }

    public Dimension getDimension() { // bad naming
        // throw if not defined...
        return dimension;
    }

    public int getLength() {
        return length;
    }

    public void define(NetcdfFileWriteable writer) {
        dimension = writer.addDimension(name, length);
    }

    public void prepare() {
        length = 0;
    }

    public void addValueToBuffer(Object value) {
        ++length;
    }

    public String getName() {
        return name;
    }
}

