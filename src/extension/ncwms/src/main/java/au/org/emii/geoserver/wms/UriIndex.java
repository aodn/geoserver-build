package au.org.emii.geoserver.wms;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UriIndex {
    // If timstamp is null, returns last URL sorted by time
    String getUrlForTimestamp(LayerDescriptor layerDescriptor, String timestamp) throws IOException;

    List<String> getTimesForDay(LayerDescriptor layerDescriptor, String day) throws IOException;

    Map<String, Map<String, Set<String>>> getUniqueDates(LayerDescriptor layerDescriptor) throws IOException;
}
