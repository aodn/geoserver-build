package au.org.emii.wps.gogoduck.index;

import au.org.emii.download.DownloadRequest;
import au.org.emii.wps.gogoduck.exception.GoGoDuckException;
import au.org.emii.wps.gogoduck.parameter.SubsetParameters;

import java.util.Set;

public interface IndexReader {
    Set<DownloadRequest> getDownloadList(String profile, String timeField, String sizeField, String urlField, SubsetParameters subset) throws GoGoDuckException;
}
