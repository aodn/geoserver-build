package au.org.emii.ncdfgenerator;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.NetcdfFileWriter;

class DimensionImpl implements IDimension {
    private static final boolean IS_SHARED = true;
    private static final boolean IS_NOT_VARIABLE_LENGTH = false;

    private final String name;
    private final boolean isUnlimited;
    private int size;
    private Dimension dimension;

    public DimensionImpl(String name, boolean isUnlimited) {
        this.name = name; // required to encode dimension
        this.isUnlimited = isUnlimited;
        this.size = 0;
    }

    public Dimension getDimension() { // bad naming
        // throw if not defined...
        return dimension;
    }

    public int getLength() {
        return size;
    }

    public void define(NetcdfFileWriter writer) {
        dimension = writer.addDimension(null, name, 0, IS_SHARED, isUnlimited, IS_NOT_VARIABLE_LENGTH);
    }

    public void prepare() {
        size = 0;
    }

    public void addValueToBuffer(Object value) {
        ++size;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isUnlimited() {
        return isUnlimited;
    }
}

