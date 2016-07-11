package au.org.emii.gogoduck.worker;

public interface IndexReader {
    public URIList getUriList(String profile, String timeField, String urlField, GoGoDuckSubsetParameters subset) throws GoGoDuckException;
}
