package au.org.emii.geoserver.wfs.response;

import java.util.Iterator;
import java.util.List;

public interface CsvSource extends Iterator<List<Object>> {
    List<String> getColumnNames();

    void close();
}
