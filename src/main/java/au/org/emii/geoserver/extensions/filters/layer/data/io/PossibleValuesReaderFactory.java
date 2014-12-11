package au.org.emii.geoserver.extensions.filters.layer.data.io;

import javax.sql.DataSource;

public class PossibleValuesReaderFactory {

    public static PossibleValuesReader getReader(DataSource dataSource, String layerName, String schemaName) {
        String ciDriverName = DatabaseDriverName.getName(dataSource, layerName, schemaName).toLowerCase();
        if (ciDriverName.contains("postgres")) {
            return new PostgresSqlPossibleValuesReader(dataSource, layerName, schemaName);
        }
        // Your driver implementation returned here

        return new NullPossibleValuesReader();
    }
}
