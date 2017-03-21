package au.org.emii.aggregator.overrides;

import com.thoughtworks.xstream.XStream;

import java.nio.file.Path;

/**
 * Class to read aggregation config
 */
public class AggregationOverridesReader {
    public static AggregationOverrides load(Path location) {
        XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.processAnnotations(AggregationOverrides.class);
        return (AggregationOverrides) xStream.fromXML(location.toFile());
    }
}
