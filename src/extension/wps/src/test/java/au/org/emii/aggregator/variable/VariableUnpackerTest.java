package au.org.emii.aggregator.variable;

import static au.org.emii.util.TestResource.open;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import au.org.emii.aggregator.variable.datatype.NumericTypes;
import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;

/**
 * Unit Tests for unpacking variables/changing their type.
 */
public class VariableUnpackerTest {

    private static final int MAX_CHUNK_SIZE = 100 * 1024 * 1024; // 100 MiB

    @Test @Ignore  // Used to confirm changes to unpacker haven't affected performance - not a real test
    public void performanceTest() throws IOException, InvalidRangeException {
        NetcdfFile dataset = open("20160714152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc");

        try {
            Variable variable = dataset.findVariable(null, "sea_surface_temperature");
            VariableUnpacker unpacker = new VariableUnpacker(variable);
            Date start = new Date();
            for (int i=0; i<50; i++) {
                FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(variable.getShape());
                long maxChunkElems = MAX_CHUNK_SIZE / variable.getElementSize();

                while (index.currentElement() < index.getSize()) {
                    int[] chunkOrigin = index.getCurrentCounter();
                    int[] chunkShape = index.computeChunkShape(maxChunkElems);
                    System.out.println("Reading " + new Section(chunkOrigin, chunkShape));
                    Array data = unpacker.read(chunkOrigin, chunkShape);
                    index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));
                }
            }

            Date end = new Date();
            System.out.println((end.getTime() - start.getTime())/50);
        } finally {
            dataset.close();
        }
    }

    @Test
    public void noConversionRequiredTest() throws IOException, InvalidRangeException {
        NetcdfFile dataset = open("IMOS_ACORN_V_20090827T163000Z_CBG_FV00_1-hour-avg.nc");

        try {
            VariableUnpacker unpacker = new VariableUnpacker(dataset.findVariable(null, "UCUR"));

            Array data = unpacker.read(new int[] {0, 40, 41}, new int[] {1, 1, 5});

            assertEquals(DataType.FLOAT, data.getDataType());

            assertArrayEquals(
                new float[] {999999.0f, -0.051123682f, -0.032568086f, -0.0046766205f, 999999.0f},
                (float[])data.get1DJavaArray(float.class), 0.0f
            );

            assertEquals(999999.0f, unpacker.getFillerValue());
            assertEquals(-10f, unpacker.getValidMin());
            assertEquals(10f, unpacker.getValidMax());
            assertNull(unpacker.getValidRange());
            assertNull(unpacker.getMissingValues());
        } finally {
            dataset.close();
        }
    }

    @Test
    public void unpackingTest() throws IOException, InvalidRangeException {
        NetcdfFile dataset = open("20160714152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc");

        try {
            VariableUnpacker unpacker = new VariableUnpacker(dataset.findVariable(null, "sea_surface_temperature"));

            Array data = unpacker.read(new int[] {0, 436, 2846}, new int[] {1, 1, 2});

            assertEquals(DataType.DOUBLE, data.getDataType());

            assertArrayEquals(
                new double[] {NumericTypes.defaultFillValue(DataType.DOUBLE).doubleValue(), 302.3891989849508},
                (double[])data.get1DJavaArray(double.class), 0.0
            );

            assertEquals(NumericTypes.defaultFillValue(DataType.DOUBLE).doubleValue(), unpacker.getFillerValue());
            assertEquals(-35.74079345725477, unpacker.getValidMin());
            assertEquals(619.5991918947548, unpacker.getValidMax());
            assertNull(unpacker.getValidRange());
            assertNull(unpacker.getMissingValues());
        } finally {
            dataset.close();
        }
    }

    @Test
    public void overridesTest() throws IOException, InvalidRangeException {
        NetcdfFile dataset = open("20160714152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc");

        try {
            UnpackerOverrides overrides = new UnpackerOverrides.Builder()
                .newFillerValue(-1.0)
                .newValidMin(0.0)
                .newValidMax(600.0)
                .newDataType(DataType.FLOAT)
                .build();

            VariableUnpacker unpacker = new VariableUnpacker(dataset.findVariable(null, "sea_surface_temperature"), overrides);

            Array data = unpacker.read(new int[] {0, 436, 2846}, new int[] {1, 1, 2});

            assertEquals(DataType.FLOAT, data.getDataType());

            assertArrayEquals(
                new float[] {-1.0f, 302.3891989849508f},
                (float[])data.get1DJavaArray(float.class), 0.0f
            );

            assertEquals(-1.0f, unpacker.getFillerValue());
            assertEquals(0.0f, unpacker.getValidMin());
            assertEquals(600.0f, unpacker.getValidMax());
            assertNull(unpacker.getValidRange());
            assertNull(unpacker.getMissingValues());
        } finally {
            dataset.close();
        }
    }
}