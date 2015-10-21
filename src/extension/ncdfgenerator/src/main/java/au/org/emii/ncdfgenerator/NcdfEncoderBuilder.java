package au.org.emii.ncdfgenerator;

import java.sql.Connection;
import org.geotools.jdbc.JDBCDataStore;

import au.org.emii.ncdfgenerator.cql.ExprParser;
import au.org.emii.ncdfgenerator.cql.IDialectTranslate;
import au.org.emii.ncdfgenerator.cql.IExprParser;
import au.org.emii.ncdfgenerator.cql.PGDialectTranslate;


public class NcdfEncoderBuilder {
    // assemble the NcdfEncoder

    private String tmpCreationDir;
    private NcdfDefinition definition;
    private String filterExpr;
    private JDBCDataStore dataStore;
    private String schema;

    public NcdfEncoderBuilder() {
    }

    public final NcdfEncoder create() throws Exception {

        if (tmpCreationDir == null) {
           throw new IllegalArgumentException("tmpCreationDir not set");
        }
        else if (definition == null) {
           throw new IllegalArgumentException("definition not set");
        }
        else if (dataStore == null) {
           throw new IllegalArgumentException("dataStore not set");
        }

        IExprParser parser = new ExprParser();
        IDialectTranslate translate = new PGDialectTranslate();
        ICreateWritable createWritable = new CreateWritable(tmpCreationDir);
        IAttributeValueParser attributeValueParser = new AttributeValueParser();

        return new NcdfEncoder(parser, translate, dataStore, schema, createWritable, attributeValueParser, definition, filterExpr);
    }

    public final NcdfEncoderBuilder setTmpCreationDir(String tmpCreationDir) {
        this.tmpCreationDir = tmpCreationDir;
        return this;
    }

    public final NcdfEncoderBuilder setDefinition(NcdfDefinition definition) {
        this.definition = definition;
        return this;
    }

    public final NcdfEncoderBuilder setFilterExpr(String filterExpr) {
        this.filterExpr = filterExpr;
        return this;
    }

    public final NcdfEncoderBuilder setDataStore(JDBCDataStore dataStore) {
        this.dataStore = dataStore;
        return this;
    }

    public final NcdfEncoderBuilder setSchema(String schema) {
        this.schema = schema;
        return this;
    }
}
