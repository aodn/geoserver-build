package au.org.emii.geoserver.wms;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.io.IOUtils;
import org.geotools.util.logging.Logging;
import org.joda.time.DateTime;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.URL;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Ncwms {
    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters");

    private static String timeFieldName = "time";
    private static String urlFieldName= "file_url";

    public static Map<String, String> urlSubstitutions = new HashMap<String, String>();
    private static String wfsServer = "http://localhost:8080/geoserver/ows";

    public Ncwms() {}

    public void sayHello(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.getOutputStream().write( getWfsServer().getBytes() );
    }

    public void setUrlFieldName(String urlFieldName1) { urlFieldName = urlFieldName1; }
    public String getUrlFieldName() { return urlFieldName; }

    public void setTimeFieldName(String timeFieldName1) { timeFieldName = timeFieldName1; }
    public String getTimeFieldName() { return timeFieldName; }

    public void setWfsServer(String wfsServer1) { wfsServer = wfsServer1; }
    public String getWfsServer() { return wfsServer; }

    public void setUrlSubstitutions(Map<String, String> urlSubstitutions1) { urlSubstitutions = urlSubstitutions1; }
    public Map<String, String> getUrlSubstitutions() { return urlSubstitutions; }

    public void getMetadata(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("1");
        LOGGER.log(Level.INFO, "GetMetadata");
        final String layerAndVariable = request.getParameter("layerName"); // TOOD needs to be case insensitive
        System.out.println("2");
        final String wfsLayer = layerAndVariable.split("/")[0];
        final String variable = layerAndVariable.split("/")[1];
        final String item = request.getParameter("item");
        System.out.println("3");
        System.out.println(item);

        if (item != null && item.compareTo("timesteps") == 0) {
            System.out.println("layerDetails");
            String day = request.getParameter("day");
            String cqlFilter = cqlFilterForSameDay(day, getTimeFieldName());
            String urlParameters = getWfsUrlParameters(wfsLayer, timeFieldName, cqlFilter);
            URL url = new URL(getWfsServer() + "?" + urlParameters);
            LOGGER.log(Level.INFO, String.format("Getting times of day from '%s'", url));

            JSONObject resultJson = new JSONObject();
            resultJson.put("timesteps", getTimesForDay(url.openConnection().getInputStream()));
            response.getOutputStream().write(resultJson.toString().getBytes());
        }
        else if (item != null && item.compareTo("layerDetails") == 0) {
            System.out.println("4");
            LOGGER.log(Level.INFO, "Returning all available dates");
            System.out.println("layerDetails");
            String urlParameters = getWfsUrlParameters(wfsLayer, timeFieldName, null);
            URL url = new URL(getWfsServer() + "?" + urlParameters);
            System.out.println(url.toString());
            url.openConnection().getInputStream();
            LOGGER.log(Level.INFO, String.format("Returning all available dates from '%s'", url));

            JSONObject resultJson = new JSONObject();
            resultJson.put("datesWithData", getUniqueDates(url.openConnection().getInputStream()));
            resultJson.put("supportedStyles", getSupportedStyles());
            resultJson.put("palettes", getPalettes());
            System.out.println(resultJson.toString());
            response.getOutputStream().write(resultJson.toString().getBytes());
        }
    }

    public void getMap(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("0");
        String layerAndVariable = request.getParameter("LAYERS"); // TOOD needs to be case insensitive
        String wfsLayer = layerAndVariable.split("/")[0];
        String variable = layerAndVariable.split("/")[1];

        System.out.println("1");

        String time = request.getParameter("TIME");

        // By default form a query to get the last file ordered by timestamp (reverse)
        String extraUrlParameters = "&" + String.format("sortBy=%s+D", timeFieldName); // Sort by time, descending
        String cqlFilter = null;
        if (time != null && time != "") {
            cqlFilter = cqlFilterForTimestamp(time, getTimeFieldName());
            System.out.println(String.format("Adding CQL_FILTER=%s", cqlFilter));
            extraUrlParameters = "";
        }

        System.out.println("2");
        String urlParameters = getWfsUrlParameters(wfsLayer, getUrlFieldName(), cqlFilter);
        urlParameters += "&" + "maxFeatures=1";
        urlParameters += extraUrlParameters;
        URL url = new URL(getWfsServer() + "?" + urlParameters);
        System.out.println(url);

        try {
            LOGGER.log(Level.INFO, String.format("Accessing '%s'", url));

            String wmsUrlStr = getWmsUrl(url.openConnection().getInputStream(), getUrlSubstitutions());
            System.out.println(wmsUrlStr);

            Map<String, String[]> wmsParameters = new HashMap(request.getParameterMap());
            wmsParameters.remove("TIME");
            wmsParameters.put("LAYERS", new String[] { variable });
            System.out.println(wmsParameters);

            String queryString = encodeMapForRequest(wmsParameters);
            System.out.println(queryString);

            URL wmsUrl = new URL(wmsUrlStr + "?" + queryString);

            IOUtils.copy(wmsUrl.openConnection().getInputStream(), response.getOutputStream());
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Problem proxying url '%s'", url));
            e.printStackTrace();
        }
    }

    private String encodeMapForRequest(Map<String, String[]> params) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                if (sb.length() != 0) sb.append('&');
                sb.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8.name()));
                sb.append('=');
                sb.append(URLEncoder.encode(param.getValue()[0], StandardCharsets.UTF_8.name()));
            }
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error encoding parameters: '%s'", e.getMessage()));
        }

        return sb.toString();
    }

    private List<String> getSupportedStyles() {
        return new ArrayList<String>() {{
            add("barb");
            add("fancyvec");
            add("trivec");
            add("stumpvec");
            add("linevec");
            add("vector");
            add("boxfill");
        }};
    }

    private List<String> getPalettes() {
        return new ArrayList<String>() {{
            add("redblue");
            add("alg");
            add("greyscale");
            add("alg2");
            add("ncview");
            add("occam");
            add("rainbow");
            add("sst_36");
            add("ferret");
            add("occam_pastel-30");
        }};
    }

    private static String cqlFilterForTimestamp(String timestamp, String timeFieldName) {
        LOGGER.log(Level.INFO, String.format("Returning cql for timestamp '%s'", timestamp));
        return String.format("%s = %s", timeFieldName, timestamp);
    }

    private static String cqlFilterForSameDay(String day, String timeFieldName) {
        String timeStart = day;
        String timeEnd = getNextDay(timeStart);
        LOGGER.log(Level.INFO, String.format("Returning times of day '%s'", day));

        String cqlFilter = String.format(
            "%s >= %s AND %s < %s",
            timeFieldName, timeStart,
            timeFieldName, timeEnd
        );

        return cqlFilter;
    }

    private static String getWfsUrlParameters(String wfsLayer, String propertyName, String cqlFilter) throws UnsupportedEncodingException {
        String urlParameters =
            String.format(
                "typeName=%s&SERVICE=WFS&outputFormat=csv&REQUEST=GetFeature&VERSION=1.0.0&PROPERTYNAME=%s",
                wfsLayer, propertyName
            );

        if (cqlFilter != null && cqlFilter != "") {
            urlParameters += String.format("&CQL_FILTER=%s", URLEncoder.encode(cqlFilter, StandardCharsets.UTF_8.name()));
        }

        return urlParameters;
    }

    private static String getTimeFromDate(String date) {
        DateTime jodaDate = new DateTime(date);
        return jodaDate.toLocalTime().toString() + "Z";
    }

    private static String getNextDay(String date) {
        DateTime jodaDate = new DateTime(date);
        return jodaDate.plusDays(1).toString();
    }

    public static CSVReader processCsvInput(InputStream csv) throws IOException {
        CSVReader csvReader = new CSVReader(new InputStreamReader(csv, StandardCharsets.UTF_8.name()));

        csvReader.readNext(); // Skip first line, it's the header
        return csvReader;
    }

    public static Map<Integer, Map<Integer, Set<Integer>> > getUniqueDates(InputStream csv) throws IOException {
        CSVReader csvReader = processCsvInput(csv);

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

    public static List<String> getTimesForDay(InputStream csv) throws IOException {
        CSVReader csvReader = processCsvInput(csv);

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

    public static String getWmsUrl(InputStream csv, Map<String, String> urlSubstitutions) throws IOException {
        CSVReader csvReader = processCsvInput(csv);

        String url = "";
        for (String[] currentRow = csvReader.readNext();
            currentRow != null;
            currentRow = csvReader.readNext()) {

            url = currentRow[1];
            // Apply ugly replacing
            for (final String search : urlSubstitutions.keySet()) {
                final String replace = urlSubstitutions.get(search);
                url = url.replaceAll(search, replace);
            }
        }

        return url;
    }
}
