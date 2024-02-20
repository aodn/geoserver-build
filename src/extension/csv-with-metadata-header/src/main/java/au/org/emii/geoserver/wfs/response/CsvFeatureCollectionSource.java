package au.org.emii.geoserver.wfs.response;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;

import java.util.ArrayList;
import java.util.List;

public class CsvFeatureCollectionSource implements CsvSource {

    private final SimpleFeatureCollection featureCollection;
    private final FeatureIterator featureIterator;

    public CsvFeatureCollectionSource(SimpleFeatureCollection featureCollection) {
        this.featureCollection = featureCollection;
        this.featureIterator = featureCollection.features();
    }

    @Override
    public List<String> getColumnNames() {
        List<String> result = new ArrayList<>();
        result.add("FID");
        SimpleFeatureType ft = featureCollection.getSchema();

        for (AttributeDescriptor descriptor: ft.getAttributeDescriptors()) {
            result.add(descriptor.getLocalName());
        }

        return result;
    }

    @Override
    public boolean hasNext() {
        return featureIterator.hasNext();
    }

    @Override
    public List<Object> next() {
        List<Object> result = new ArrayList<>();
        SimpleFeature f = (SimpleFeature)featureIterator.next();
        result.add(f.getIdentifier().getID());

        for (Object attribute: f.getAttributes()) {
            result.add(attribute);
        }

        return result;
    }

    @Override
    public void close() {
        featureIterator.close();
    }
}
