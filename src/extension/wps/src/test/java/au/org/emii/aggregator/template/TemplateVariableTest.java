package au.org.emii.aggregator.template;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for TemplateVariable
 */
public class TemplateVariableTest {

    @Test
    public void limitedVariableTest() throws InvalidRangeException {
        Array data = Array.factory(DataType.DOUBLE, new int[] {1, 2});

        data.setDouble(0, 33.0);
        data.setDouble(1, 74.0);

        List<Dimension> dimensions = new ArrayList<>();

        dimensions.add(new Dimension("latitude", 1));
        dimensions.add(new Dimension("longitude", 2));

        TemplateVariable variable = new TemplateVariable("temp", new ArrayList<Attribute>(), dimensions, null, data);

        int[] shape = variable.getShape();
        int[] origin = new int[shape.length];

        Array staticData = variable.read(origin, shape);

        assertArrayEquals(new int[] {1, 2}, staticData.getShape());
        assertEquals(33.0, staticData.getDouble(0), 0.0);
        assertEquals(74.0, staticData.getDouble(1), 0.0);
    }

}