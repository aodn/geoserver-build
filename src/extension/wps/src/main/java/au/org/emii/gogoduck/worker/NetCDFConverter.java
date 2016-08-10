package au.org.emii.gogoduck.worker;

import au.org.emii.core.Config;
import au.org.emii.gogoduck.exception.GoGoDuckException;
import au.org.emii.utils.GoGoDuckConfig;

import java.nio.file.Path;

public class NetCDFConverter extends Converter {

    final static String MIME_TYPE = "application/x-netcdf";
    private final static String EXTENSION = "nc";

    @Override
    public Path convert(Path outputFile, GoGoDuckModule module) throws GoGoDuckException {
        // No conversion necessary
        return outputFile;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public String getExtension() {
        return EXTENSION;
    }

}
