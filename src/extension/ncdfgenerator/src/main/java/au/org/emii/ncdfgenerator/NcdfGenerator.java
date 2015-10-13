package au.org.emii.ncdfgenerator;

import java.io.OutputStream;
import java.sql.Connection;

public class NcdfGenerator {

    private final NcdfEncoderBuilder encoderBuilder;

    public NcdfGenerator(String layerConfigDir, String tmpCreationDir) {
        encoderBuilder = new NcdfEncoderBuilder();
        encoderBuilder.setLayerConfigDir(layerConfigDir);
        encoderBuilder.setTmpCreationDir(tmpCreationDir);
        encoderBuilder.setOutputType(new ZipFormatter());
    }

    // Method must not be final to allow mocking for testing purposes
    public void write(String typename, String filterExpr, Connection conn, OutputStream os) throws Exception {
        try {
            NcdfEncoder encoder = encoderBuilder.create(typename, filterExpr, conn, os);
            encoder.write();
        }
        finally {
            os.close();
            conn.close();
        }
    }
}

