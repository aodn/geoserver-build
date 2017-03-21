package au.org.emii.wps.gogoduck.converter;

import au.org.emii.wps.gogoduck.converter.TextCsvConverter;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import ucar.nc2.dataset.CoordinateAxis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TextCsvConverterTest {

    private static final Path TIME_SERIES_INPUT_FILE = Paths.get("src/test/resources/timeseries.nc");
    private static final File TIME_SERIES_OUTPUT_FILE = new File("src/test/resources/expected_timeseries.csv");

    @Test
    public void testCsvGeneration() throws IOException {
        TextCsvConverter converter = new TextCsvConverter();

        Path outputFile = Files.createTempFile("converter", ".csv");
        Files.deleteIfExists(outputFile);

        try {
            CoordinateAxis coordinateAxis = mock(CoordinateAxis.class);
            when(coordinateAxis.getShortName()).thenReturn("TIME");

            converter.convert(TIME_SERIES_INPUT_FILE, outputFile);
            assertTrue("CSV file generated differs from expected file", FileUtils.contentEquals(outputFile.toFile(), TIME_SERIES_OUTPUT_FILE));
        } finally {
            Files.delete(outputFile);
        }
    }
}
