package au.org.emii.geoserver.wms;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface URLIndexInterface {
    // If timstamp is null, returns last URL sorted by time
    public String getUrlForTimestamp(LayerDescriptor layerDescriptor, String timestamp) throws IOException;

    public List<String> getTimesForDay(LayerDescriptor layerDescriptor, String day) throws IOException;

    public Map<Integer, Map<Integer, Set<Integer>>> getUniqueDates(LayerDescriptor layerDescriptor) throws IOException;
}
