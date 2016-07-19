package au.org.emii.gogoduck.worker;

import java.util.Properties;

public interface IndexReader {
    URIList getUriList(String profile, String timeField, String urlField, GoGoDuckSubsetParameters subset, Properties properties) throws GoGoDuckException;
}
