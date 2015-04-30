
package au.org.emii.ncdfgenerator;

import java.sql.Connection;
import java.io.OutputStream;


public class NcdfGenerator {

    private final NcdfEncoderBuilder encoderBuilder;

    public NcdfGenerator(String layerConfigDir, String tmpCreationDir) {
        encoderBuilder = new NcdfEncoderBuilder();
        encoderBuilder.setLayerConfigDir(layerConfigDir);
        encoderBuilder.setTmpCreationDir(tmpCreationDir);
        encoderBuilder.setOutputType(new ZipFormatter());
    }

    public final void write(String typename, String filterExpr, Connection conn, OutputStream os) throws Exception {
        try {
            NcdfEncoder encoder = encoderBuilder.create(typename, filterExpr, conn, os);
            encoder.write();
        } finally {
            os.close();
            conn.close();
        }
    }
}

