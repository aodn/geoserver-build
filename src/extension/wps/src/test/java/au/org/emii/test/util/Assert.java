package au.org.emii.test.util;

import org.apache.commons.io.FileUtils;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * Created by craigj on 15/02/17.
 */
public class Assert {
    public static void assertNetcdfFilesEqual(Path expected, Path actual) throws IOException {
        String expectedCdl = getAsCdl(expected);
        String actualCdl = getAsCdl(actual);
        assertEquals(expectedCdl, actualCdl);
    }

    public static void assertNetcdfFileEqualsCdl(Path expected, Path actual) throws IOException {
        String expectedCdl = FileUtils.readFileToString(expected.toFile());
        String actualCdl = getAsCdl(actual);
        assertEquals(expectedCdl, actualCdl);
    }

    private static String getAsCdl(Path netcdfFile) throws IOException {
        NetcdfFile expectedFile = NetcdfFile.open(netcdfFile.toAbsolutePath().toString());
        StringWriter outputWriter = new StringWriter();
        NCdumpW.print(expectedFile, outputWriter, NCdumpW.WantValues.all, false, false, null, null);
        return outputWriter.toString().replaceFirst("netcdf .* ", "netcdf "); // return cdl minus filename
    }
}
