
package au.org.emii.ncdfgenerator;

import au.org.emii.ncdfgenerator.cql.ExprParser;
import au.org.emii.ncdfgenerator.cql.IExprParser;
import au.org.emii.ncdfgenerator.cql.IDialectTranslate;
import au.org.emii.ncdfgenerator.cql.PGDialectTranslate;

import java.sql.Connection;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;


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

