package au.org.emii.util;

import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Testing resource utility methods
 */
public class TestResource {

    public static Path get(String resource) {
        try {
            URL url = TestResource.class.getResource("/"+resource);
            return java.nio.file.Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError("Unexpected error opening test resource", e);
        }
    }

    public static NetcdfFile open(String resource) throws IOException {
        Path testFile = TestResource.get(resource);
        return NetcdfFile.open(testFile.toString());
    }

}
