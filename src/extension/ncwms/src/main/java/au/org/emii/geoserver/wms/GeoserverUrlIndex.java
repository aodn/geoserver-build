package au.org.emii.geoserver.wms;

import org.geoserver.catalog.Catalog;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeoserverUrlIndex implements UriIndex {
    static Logger LOGGER = Logging.getLogger(GeoserverUrlIndex.class);

    private Catalog catalog;

    public GeoserverUrlIndex(Catalog catalog) {
        this.catalog = catalog;
    }

    public String getUrlForTimestamp(LayerDescriptor layerDescriptor, String timestamp) throws IOException {
        Query query = new Query(layerDescriptor.geoserverName());

        if (timestamp != null && timestamp.compareTo("") != -1) {
            query.setFilter(cqlFilterForTimestamp(timestamp, layerDescriptor.getTimeFieldName()));
        } else {
            // By default form a query to get the last file ordered by timestamp (reverse)
            FilterFactory ff = CommonFactoryFinder.getFilterFactory();
            SortBy sortBy = ff.sort(layerDescriptor.getTimeFieldName(), SortOrder.DESCENDING);
            query.setSortBy(new SortBy[]{sortBy});
        }

        query.setMaxFeatures(1);
        query.setPropertyNames(new String[]{layerDescriptor.getUrlFieldName()});

        SimpleFeatureIterator iterator = getFeatures(layerDescriptor.geoserverName(), query);

        String url = null;

        try {
             SimpleFeature feature = iterator.next();
             url = (String) feature.getAttribute(layerDescriptor.getUrlFieldName());
        } finally {
            iterator.close();
        }

        return url;
    }

    public List<String> getTimesForDay(LayerDescriptor layerDescriptor, String day) throws IOException {
        Query query = new Query(layerDescriptor.geoserverName());
        query.setFilter(cqlFilterForSameDay(day, layerDescriptor.getTimeFieldName()));
        query.setPropertyNames(new String[]{layerDescriptor.getTimeFieldName()});

        List<String> timesOfDay = new ArrayList<>();

        SimpleFeatureIterator iterator = getFeatures(layerDescriptor.geoserverName(), query);

        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Date timestamp = (Date) feature.getAttribute(layerDescriptor.getTimeFieldName());
                LOGGER.log(Level.INFO, String.format("Processing timestamp '%s'", timestamp));
                timesOfDay.add(getTimeFromDate(timestamp));
            }
        } finally {
            iterator.close();
        }

        return timesOfDay;
    }

    public Map<String, Map<String, Set<String>>> getUniqueDates(LayerDescriptor layerDescriptor) throws IOException {
        Query query = new Query(layerDescriptor.geoserverName());
        query.setPropertyNames(new String[]{layerDescriptor.getTimeFieldName()});

        Map<String, Map<String, Set<String> > > dates = new HashMap<>();

        SimpleFeatureIterator iterator = getFeatures(layerDescriptor.geoserverName(), query);

        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();

                Date timestamp = (Date) feature.getAttribute(layerDescriptor.getTimeFieldName());

                if (timestamp == null) continue;

                LOGGER.log(Level.INFO, String.format("Processing timestamp '%s'", timestamp));
                DateTime date = new DateTime(timestamp.getTime());

                Integer yearInt = date.getYear();
                Integer monthInt = (date.getMonthOfYear() - 1); // Zero indexed
                Integer dayInt = date.getDayOfMonth();

                String year = yearInt.toString();
                String month = monthInt.toString();
                String day = dayInt.toString();

                if (!dates.containsKey(year)) {
                    dates.put(year, new HashMap<String, Set<String> >());
                }

                if (!dates.get(year).containsKey(month)) {
                    dates.get(year).put(month, new HashSet<String>());
                }

                dates.get(year).get(month).add(day);
            }
        } finally {
            iterator.close();
        }

        return dates;
    }

    private Filter cqlFilterForTimestamp(String timestamp, String timeFieldName) {
        LOGGER.log(Level.INFO, String.format("Returning cql for timestamp '%s'", timestamp));
        try {
            return CQL.toFilter(String.format("%s = '%s'", timeFieldName, timestamp));
        } catch (CQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Filter cqlFilterForSameDay(String day, String timeFieldName) {
        DateTime timeStart = new DateTime(day);
        DateTime timeEnd = timeStart.plusDays(1); // Just the next day
        LOGGER.log(Level.INFO, String.format("Returning times of day '%s'", day));

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        try {
            return CQL.toFilter(String.format(
                "%s >= '%s' AND %s < '%s'",
                timeFieldName, formatter.print(timeStart.withZone(DateTimeZone.UTC)),
                timeFieldName, formatter.print(timeEnd.withZone(DateTimeZone.UTC))
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SimpleFeatureIterator getFeatures(String typeName, Query query) throws IOException {
        SimpleFeatureSource featureSource = (SimpleFeatureSource) catalog.getFeatureTypeByName(typeName).getFeatureSource(null, null);
        SimpleFeatureCollection collection = featureSource.getFeatures(query);
        return collection.features();
    }

    private String getTimeFromDate(Date timestamp) {
        DateTime jodaDate = new DateTime(timestamp);
        return jodaDate.toLocalTime().toString() + "Z";
    }
}
