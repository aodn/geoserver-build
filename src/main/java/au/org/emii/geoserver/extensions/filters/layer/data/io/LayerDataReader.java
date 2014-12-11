package au.org.emii.geoserver.extensions.filters.layer.data.io;

import org.geotools.util.logging.Logging;

import javax.sql.DataSource;
import java.util.logging.Logger;

public abstract class LayerDataReader {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters.layer.data.io");

    private DataSource dataSource;
    private String layerName;
    private String schemaName;

    public LayerDataReader(DataSource dataSource, String layerName, String schemaName) {
        this.dataSource = dataSource;
        this.layerName = layerName;
        this.schemaName = schemaName;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    protected String getLayerName() {
        return layerName;
    }

    protected String getSchemaName() {
        return schemaName;
    }

}
