package au.org.emii.gogoduck.worker;

import java.nio.file.Path;

public class NetCDFConverter extends Converter {

    final static String MIME_TYPE = "application/x-netcdf";
    private final static String EXTENSION = "nc";

    @Override
    public Path convert(Path outputFile) throws GoGoDuckException {
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
