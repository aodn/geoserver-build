package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;

import java.util.List;

public interface PossibleValuesReader {

    public void read(List<Filter> filters);
}
