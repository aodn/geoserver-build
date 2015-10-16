package au.org.emii.geoserver.wms;

import au.com.bytecode.opencsv.CSVReader;
import org.geotools.util.logging.Logging;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class URLIndexWFSHttp implements URLIndexInterface {
    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.wms.URLIndexWFSHttp");

    private static String timeFieldName = "time";
    private static String urlFieldName= "file_url";
    private static String wfsServer = "http://localhost:8080/geoserver/ows";

    public void setUrlFieldName(String urlFieldName) { URLIndexWFSHttp.urlFieldName = urlFieldName; }
    public String getUrlFieldName() { return urlFieldName; }

    public void setTimeFieldName(String timeFieldName) { URLIndexWFSHttp.timeFieldName = timeFieldName; }
    public String getTimeFieldName() { return timeFieldName; }

    public void setWfsServer(String wfsServer) { URLIndexWFSHttp.wfsServer = wfsServer; }
    public String getWfsServer() { return wfsServer; }

    public String getUrlForTimestamp(LayerDescriptor layerDescriptor, String timestamp) throws IOException {
        // By default form a query to get the last file ordered by timestamp (reverse)
        String extraUrlParameters = "&" + String.format("sortBy=%s+D", timeFieldName); // Sort by time, descending
        String cqlFilter = null;
        if (timestamp != null && timestamp != "") {
            cqlFilter = cqlFilterForTimestamp(timestamp, timeFieldName);
            extraUrlParameters = "";
        }

        String urlParameters = getWfsUrlParameters(layerDescriptor.geoserverName(), timeFieldName, cqlFilter);
        urlParameters += "&" + "maxFeatures=1";
        urlParameters += extraUrlParameters;

        CSVReader csvReader = processCsvInput(wfsQuery(urlParameters));

        String wmsUrl = "";
        String[] currentRow = csvReader.readNext();
        return currentRow[1];
    }

    public List<String> getTimesForDay(LayerDescriptor layerDescriptor, String day) throws IOException {
        String cqlFilter = cqlFilterForSameDay(day, getTimeFieldName());
        String urlParameters = getWfsUrlParameters(layerDescriptor.geoserverName(), timeFieldName, cqlFilter);

        CSVReader csvReader = processCsvInput(wfsQuery(urlParameters));

        List<String> timesOfDay = new ArrayList<String>();

        for (String[] currentRow = csvReader.readNext();
             currentRow != null;
             currentRow = csvReader.readNext()) {

            LOGGER.log(Level.INFO, String.format("Processing row '%s'", currentRow));
            String time = getTimeFromDate(currentRow[1]);
            timesOfDay.add(time);
        }

        return timesOfDay;
    }

    public Map<Integer, Map<Integer, Set<Integer>> > getUniqueDates(LayerDescriptor layerDescriptor) throws IOException {
        String urlParameters = getWfsUrlParameters(layerDescriptor.geoserverName(), timeFieldName, null);

        CSVReader csvReader = processCsvInput(wfsQuery(urlParameters));

        Map<Integer, Map<Integer, Set<Integer> > > dates =
                new HashMap<Integer , Map<Integer, Set<Integer> > >();

        for (String[] currentRow = csvReader.readNext();
             currentRow != null;
             currentRow = csvReader.readNext()) {

            LOGGER.log(Level.INFO, String.format("Processing row '%s'", currentRow));

            DateTime date = new DateTime(currentRow[1]);

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

    public CSVReader processCsvInput(InputStream csv) throws IOException {
        CSVReader csvReader = new CSVReader(new InputStreamReader(csv, StandardCharsets.UTF_8.name()));

        csvReader.readNext(); // Skip first line, it's the header
        return csvReader;
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
