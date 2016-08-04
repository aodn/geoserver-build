package au.org.emii.gogoduck.worker;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TextCsvConverterTest {

    private static final Path TIME_SERIES_INPUT_FILE = Paths.get("src/test/resources/timeseries.nc");
    private static final File TIME_SERIES_OUTPUT_FILE = new File("src/test/resources/expected_timeseries.csv");

    @Test
    public void testCsvGeneration() throws IOException {
        TextCsvConverter converter = new TextCsvConverter();

        Path outputFile = null;

        try {
            outputFile = converter.convert(TIME_SERIES_INPUT_FILE);
            assertTrue("CSV file generated differs from expected file", FileUtils.contentEquals(outputFile.toFile(), TIME_SERIES_OUTPUT_FILE));
        } finally {
            if (outputFile != null) {
                Files.delete(outputFile);
            }
        }
    }

}
