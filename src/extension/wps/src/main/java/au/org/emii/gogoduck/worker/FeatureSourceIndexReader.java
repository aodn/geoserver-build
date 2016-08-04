package au.org.emii.gogoduck.worker;

import au.org.emii.gogoduck.exception.GoGoDuckException;
import org.geoserver.catalog.Catalog;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class FeatureSourceIndexReader implements IndexReader {
    private static final Logger logger = LoggerFactory.getLogger(FeatureSourceIndexReader.class);

    private Catalog catalog = null;
    private UserLog userLog = null;

    public FeatureSourceIndexReader(UserLog userLog, Catalog catalog) {
        this.userLog = userLog;
        this.catalog = catalog;
    }

    @Override
    public URIList getUriList(String profile, String timeField, String urlField, GoGoDuckSubsetParameters subset) throws GoGoDuckException {

        URIList uriList = new URIList();

        String timeCoverageStart = subset.get("TIME").start;
        String timeCoverageEnd = subset.get("TIME").end;

        // TODO Should include also workspace, but works also without
        String typeName = profile;

        Filter cqlFilter = null;

        try {
            cqlFilter = CQL.toFilter(
                    String.format("%s >= '%s' AND %s <= '%s'",
                            timeField, timeCoverageStart,
                            timeField, timeCoverageEnd)
            );
        } catch (CQLException e) {
            throw new GoGoDuckException(e.getMessage());
        }

        Query query = new Query(typeName, cqlFilter, new String[]{urlField});

        SimpleFeatureIterator iterator = null;
        try {
            iterator = getFeatures(typeName, query);
        } catch (IOException e) {
            throw new GoGoDuckException(e.getMessage());
        }

        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                String url = (String) feature.getAttribute(urlField);
                logger.info(String.format("Processing url '%s'", url));
                uriList.add(new URI(url));
            }
        } catch (Exception e) {
            userLog.log("We could not obtain list of URLs, does the collection still exist?");
            throw new GoGoDuckException(String.format("Could not obtain list of URLs: '%s'", e.getMessage()));
        } finally {
            iterator.close();
        }

        return uriList;
    }

    private SimpleFeatureIterator getFeatures(String typeName, Query query) throws IOException {
        SimpleFeatureSource featureSource = (SimpleFeatureSource) catalog.getFeatureTypeByName(typeName).getFeatureSource(null, null);
        SimpleFeatureCollection collection = featureSource.getFeatures(query);
        return collection.features();
    }
}
