package au.org.emii.geoserver.wms;

import org.geotools.util.logging.Logging;
import org.joda.time.DateTime;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class URLIndexWFSHttp implements URLIndexInterface {
    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.wms.URLIndexWFSHttp");

    private static String wfsServer = "http://localhost:8080/geoserver/ows";

    public void setWfsServer(String wfsServer) { URLIndexWFSHttp.wfsServer = wfsServer; }
    public String getWfsServer() { return wfsServer; }

    public String getUrlForTimestamp(LayerDescriptor layerDescriptor, String timestamp) throws IOException {
        // By default form a query to get the last file ordered by timestamp (reverse)
        String extraUrlParameters = "&" + String.format("sortBy=%s+D", layerDescriptor.getTimeFieldName()); // Sort by time, descending
        String cqlFilter = null;
        if (timestamp != null && timestamp != "") {
            cqlFilter = cqlFilterForTimestamp(timestamp, layerDescriptor.getTimeFieldName());
            extraUrlParameters = "";
        }

        String urlParameters = getWfsUrlParameters(layerDescriptor.geoserverName(), layerDescriptor.getUrlFieldName(), cqlFilter);
        urlParameters += "&" + "maxFeatures=1";
        urlParameters += extraUrlParameters;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(wfsQuery(urlParameters)));
        bufferedReader.readLine(); // Skip header
        return bufferedReader.readLine().split(",")[1];
    }

    public List<String> getTimesForDay(LayerDescriptor layerDescriptor, String day) throws IOException {
        String cqlFilter = cqlFilterForSameDay(day, layerDescriptor.getTimeFieldName());
        String urlParameters = getWfsUrlParameters(layerDescriptor.geoserverName(), layerDescriptor.getTimeFieldName(), cqlFilter);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(wfsQuery(urlParameters)));
        bufferedReader.readLine(); // Skip header

        List<String> timesOfDay = new ArrayList<String>();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            LOGGER.log(Level.INFO, String.format("Processing row '%s'", line));
            String time = getTimeFromDate(line.split(",")[1]);
            timesOfDay.add(time);
        }

        return timesOfDay;
    }

    public Map<Integer, Map<Integer, Set<Integer>> > getUniqueDates(LayerDescriptor layerDescriptor) throws IOException {
        String urlParameters = getWfsUrlParameters(layerDescriptor.geoserverName(), layerDescriptor.getTimeFieldName(), null);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(wfsQuery(urlParameters)));
        bufferedReader.readLine(); // Skip header

        Map<Integer, Map<Integer, Set<Integer> > > dates =
                new HashMap<Integer , Map<Integer, Set<Integer> > >();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            LOGGER.log(Level.INFO, String.format("Processing row '%s'", line));

            DateTime date = new DateTime(line.split(",")[1]);

            Integer year = date.getYear();
            Integer month = (date.getMonthOfYear() - 1); // Zero indexed
            Integer day = date.getDayOfMonth();

            if (!dates.containsKey(year)) {
                dates.put(year, new HashMap<Integer, Set<Integer> >());
            }

            if (!dates.get(year).containsKey(month)) {
                dates.get(year).put(month, new HashSet<Integer>());
            }

            dates.get(year).get(month).add(day);
        }

        return dates;
    }

    private String cqlFilterForTimestamp(String timestamp, String timeFieldName) {
        LOGGER.log(Level.INFO, String.format("Returning cql for timestamp '%s'", timestamp));
        return String.format("%s = %s", timeFieldName, timestamp);
    }

    private String cqlFilterForSameDay(String day, String timeFieldName) {
        DateTime timeStart = new DateTime(day);
        DateTime timeEnd = timeStart.plusDays(1); // Just the next day
        LOGGER.log(Level.INFO, String.format("Returning times of day '%s'", day));

        return String.format(
                "%s >= %s AND %s < %s",
                timeFieldName, timeStart,
                timeFieldName, timeEnd
        );
    }

    private String getWfsUrlParameters(String wfsLayer, String propertyName, String cqlFilter) throws UnsupportedEncodingException {
        String urlParameters =
                String.format(
                        "typeName=%s&SERVICE=WFS&outputFormat=csv&REQUEST=GetFeature&VERSION=1.0.0&PROPERTYNAME=%s",
                        wfsLayer, propertyName
                );

        if (cqlFilter != null && ! cqlFilter.isEmpty()) {
            urlParameters += String.format("&CQL_FILTER=%s", URLEncoder.encode(cqlFilter, StandardCharsets.UTF_8.name()));
        }

        return urlParameters;
    }

    private String getTimeFromDate(String date) {
        DateTime jodaDate = new DateTime(date);
        return jodaDate.toLocalTime().toString() + "Z";
    }

    public InputStream wfsQuery(String urlParameters) throws IOException {
        URL url = new URL(wfsServer + "?" + urlParameters);
        LOGGER.log(Level.INFO, String.format("WFS query '%s'", url));
        return url.openConnection().getInputStream();
    }
}
