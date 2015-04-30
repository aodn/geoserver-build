
package au.org.emii.ncdfgenerator;

import au.org.emii.ncdfgenerator.cql.ExprParser;
import au.org.emii.ncdfgenerator.cql.IExprParser;
import au.org.emii.ncdfgenerator.cql.IDialectTranslate;
import au.org.emii.ncdfgenerator.cql.PGDialectTranslate;

import java.sql.Connection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;


public class NcdfEncoderBuilder {
    // responsible for assembling the NcdfEncoder

    private String layerConfigDir;
    private String tmpCreationDir;
    private IOutputFormatter outputFormatter;

    public NcdfEncoderBuilder() {
    }

    public final NcdfEncoder create(String typename, String filterExpr, Connection conn, OutputStream os) throws Exception {

        InputStream config = null;
        try {
            IExprParser parser = new ExprParser();
            IDialectTranslate translate = new PGDialectTranslate();
            ICreateWritable createWritable = new CreateWritable(tmpCreationDir);
            IAttributeValueParser attributeValueParser = new AttributeValueParser();

            config = new FileInputStream(layerConfigDir + "/" + typename + ".xml");

            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
            Node node = document.getFirstChild();
            NcdfDefinition definition = new NcdfDefinitionXMLParser().parse(node);

            return new NcdfEncoder(parser, translate, conn, createWritable, attributeValueParser, definition, filterExpr, outputFormatter, os);
        } finally {
            if(config != null)
                config.close();
            // conn.close();
        }
    }

    public final void setLayerConfigDir(String layerConfigDir) {
        this.layerConfigDir = layerConfigDir;
    }

    public final void setTmpCreationDir(String tmpCreationDir) {
        this.tmpCreationDir = tmpCreationDir;
    }

    public final void setOutputType(IOutputFormatter outputFormatter) {
        this.outputFormatter = outputFormatter;
    }
}

