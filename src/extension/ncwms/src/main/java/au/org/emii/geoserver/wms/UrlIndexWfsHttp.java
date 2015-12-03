package au.org.emii.geoserver.wms;

import org.geotools.util.logging.Logging;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UrlIndexWfsHttp implements UrlIndexInterface {
    static Logger LOGGER = Logging.getLogger(UrlIndexWfsHttp.class);

    private final String wfsServer;

    public UrlIndexWfsHttp(NcwmsConfig ncwmsConfig) {
        wfsServer = ncwmsConfig.getConfigVariable("/ncwms/wfsServer", "http://localhost:8080/geoserver/ows");
        LOGGER.log(Level.INFO, String.format("Using wfsServer '%s'", wfsServer));
    }

    public String getUrlForTimestamp(LayerDescriptor layerDescriptor, String timestamp) throws IOException {
        // By default form a query to get the last file ordered by timestamp (reverse)
        String extraUrlParameters = "&" + String.format("sortBy=%s+D", layerDescriptor.getTimeFieldName()); // Sort by time, descending
        String cqlFilter = null;
        if (timestamp != null && timestamp.compareTo("") != -1) {
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

        List<String> timesOfDay = new ArrayList<>();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            LOGGER.log(Level.INFO, String.format("Processing row '%s'", line));
            String time = getTimeFromDate(line.split(",")[1]);
            timesOfDay.add(time);
        }

        return timesOfDay;
    }

    public Map<String, Map<String, Set<String>>> getUniqueDates(LayerDescriptor layerDescriptor) throws IOException {
        String urlParameters = getWfsUrlParameters(layerDescriptor.geoserverName(), layerDescriptor.getTimeFieldName(), null);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(wfsQuery(urlParameters)));
        bufferedReader.readLine(); // Skip header

        Map<String, Map<String, Set<String> > > dates = new HashMap<>();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            LOGGER.log(Level.INFO, String.format("Processing row '%s'", line));

            DateTime date = new DateTime(line.split(",")[1]);

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

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        
        return String.format(
            "%s >= %s AND %s < %s",
            timeFieldName, formatter.print(timeStart.withZone(DateTimeZone.UTC)),
            timeFieldName, formatter.print(timeEnd.withZone(DateTimeZone.UTC))
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
        URL url = new URL(String.format("%s?%s", wfsServer, urlParameters));
        LOGGER.log(Level.INFO, String.format("WFS query '%s'", url));
        return url.openConnection().getInputStream();
    }
}
