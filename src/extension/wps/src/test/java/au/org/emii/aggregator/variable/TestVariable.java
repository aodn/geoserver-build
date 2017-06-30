package au.org.emii.aggregator.variable;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestVariable extends AbstractVariable {
    private final String name;
    private final List<Dimension> dimensions;
    private final Array data;

    public TestVariable(String name, List<Dimension> dimensions, Array data) {
        this.name = name;
        this.dimensions = dimensions;
        this.data = data;
    }

    @Override
    public String getShortName() {
        return "name";
    }

    @Override
    public boolean isUnsigned() {
        return false;
    }

    @Override
    public DataType getDataType() {
        return data.getDataType();
    }

    @Override
    public AxisType getAxisType() {
        return null;
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    @Override
    public List<Attribute> getAttributes() {
        return new ArrayList<>();
    }

    @Override
    public Array read(int[] origin, int[] shape) throws InvalidRangeException, IOException {
        Array section = data.sectionNoReduce(origin, shape, null);
        return Array.factory(section.copyToNDJavaArray());
    }
}
