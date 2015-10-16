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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Ncwms {
    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.wms.ncwms");

    private static String timeFieldName = "time";
    private static String urlFieldName= "file_url";

    public static Map<String, String> urlSubstitutions = new HashMap<String, String>();
    private static String wfsServer = "http://localhost:8080/geoserver/ows";

    public Ncwms() {}

    public void setUrlFieldName(String urlFieldName) { Ncwms.urlFieldName = urlFieldName; }
    public String getUrlFieldName() { return urlFieldName; }

    public void setTimeFieldName(String timeFieldName) { Ncwms.timeFieldName = timeFieldName; }
    public String getTimeFieldName() { return timeFieldName; }

    public void setWfsServer(String wfsServer) { Ncwms.wfsServer = wfsServer; }
    public String getWfsServer() { return wfsServer; }

    public void setUrlSubstitutions(Map<String, String> urlSubstitutions) { Ncwms.urlSubstitutions = urlSubstitutions; }
    public Map<String, String> getUrlSubstitutions() { return urlSubstitutions; }

    public void getMetadata(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "GetMetadata");
        final String layerAndVariable = request.getParameter("layerName"); // TOOD needs to be case insensitive
        String variable = layerAndVariable.split("/")[1];
        final String wfsLayer = layerAndVariable.split("/")[0];
        final String item = request.getParameter("item");

        if (item != null && item.compareTo("timesteps") == 0) {
            String day = request.getParameter("day");
            String cqlFilter = cqlFilterForSameDay(day, getTimeFieldName());
            String urlParameters = getWfsUrlParameters(wfsLayer, timeFieldName, cqlFilter);

            JSONObject resultJson = new JSONObject();
            resultJson.put("timesteps", getTimesForDay(wfsQuery(urlParameters)));
            response.getOutputStream().write(resultJson.toString().getBytes());
        }
        else if (item != null && item.compareTo("layerDetails") == 0) {
            LOGGER.log(Level.INFO, "Returning all available dates");
            String urlParameters = getWfsUrlParameters(wfsLayer, timeFieldName, null);

            Document getCapabilitiesDocument = getCapabilitiesXml(wfsLayer);

            JSONObject resultJson = new JSONObject();
            resultJson.put("datesWithData", getUniqueDates(wfsQuery(urlParameters)));
            resultJson.put("supportedStyles", getSupportedStyles(getCapabilitiesDocument, variable));
            resultJson.put("palettes", getPalettes(getCapabilitiesDocument, variable));
            response.getOutputStream().write(resultJson.toString().getBytes());
        }
    }

    public void getMap(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String layerAndVariable = request.getParameter("LAYERS"); // TOOD needs to be case insensitive
        String wfsLayer = layerAndVariable.split("/")[0];
        String variable = layerAndVariable.split("/")[1];

        String time = request.getParameter("TIME");

        String wmsUrlStr = getWmsUrl(wfsLayer, time);

        try {
            Map<String, String[]> wmsParameters = new HashMap(request.getParameterMap());
            wmsParameters.remove("TIME");
            wmsParameters.put("LAYERS", new String[] { variable });

            String queryString = encodeMapForRequest(wmsParameters);

            URL wmsUrl = new URL(wmsUrlStr + "?" + queryString);

            IOUtils.copy(wmsUrl.openConnection().getInputStream(), response.getOutputStream());
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Problem proxying url '%s'", wmsUrlStr));
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

    private List<String> getSupportedStyles(Document getCapabilitiesXml, String layerName) {
        // TODO parse getCapabilitiesXml

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

    private List<String> getPalettes(Document getCapabilitiesXml, String layerName) {
        // TODO parse getCapabilitiesXml

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

    private Document getCapabilitiesXml(String wfsLayer)
        throws IOException {
        String wmsUrl = null;
        try {
            wmsUrl = getWmsUrl(wfsLayer, null);
            String getCapabilitiesUrl =
                String.format("%s?service=WMS&version=1.3.0&request=GetCapabilities",
                    wmsUrl
                );

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new URL(getCapabilitiesUrl).openStream());

            return doc;
        }
        catch (SAXException e) {
            LOGGER.log(Level.SEVERE, String.format("Error parsing GetCapabilities XML document at '%s'", wmsUrl));
            throw new IOException(e);
        }
        catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, String.format("Error parsing GetCapabilities XML document at '%s'", wmsUrl));
            throw new IOException(e);
        }
    }

    private String cqlFilterForTimestamp(String timestamp, String timeFieldName) {
        LOGGER.log(Level.INFO, String.format("Returning cql for timestamp '%s'", timestamp));
        return String.format("%s = %s", timeFieldName, timestamp);
    }

    private String cqlFilterForSameDay(String day, String timeFieldName) {
        String timeStart = day;
        String timeEnd = getNextDay(timeStart);
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

    private String getNextDay(String date) {
        DateTime jodaDate = new DateTime(date);
        return jodaDate.plusDays(1).toString();
    }

    public CSVReader processCsvInput(InputStream csv) throws IOException {
        CSVReader csvReader = new CSVReader(new InputStreamReader(csv, StandardCharsets.UTF_8.name()));

        csvReader.readNext(); // Skip first line, it's the header
        return csvReader;
    }

    public Map<Integer, Map<Integer, Set<Integer>> > getUniqueDates(InputStream csv) throws IOException {
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

    public List<String> getTimesForDay(InputStream csv) throws IOException {
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

    public InputStream wfsQuery(String urlParameters) throws IOException {
        URL url = new URL(wfsServer + "?" + urlParameters);
        LOGGER.log(Level.INFO, String.format("WFS query '%s'", url));
        return url.openConnection().getInputStream();
    }

    public String getWmsUrl(String wfsLayer, String time) throws IOException {
        // By default form a query to get the last file ordered by timestamp (reverse)
        String extraUrlParameters = "&" + String.format("sortBy=%s+D", timeFieldName); // Sort by time, descending
        String cqlFilter = null;
        if (time != null && time != "") {
            cqlFilter = cqlFilterForTimestamp(time, timeFieldName);
            extraUrlParameters = "";
        }

        String urlParameters = getWfsUrlParameters(wfsLayer, timeFieldName, cqlFilter);
        urlParameters += "&" + "maxFeatures=1";
        urlParameters += extraUrlParameters;

        CSVReader csvReader = processCsvInput(wfsQuery(urlParameters));

        String wmsUrl = "";
        String[] currentRow = csvReader.readNext();
        return mangleUrl(currentRow[1]);
    }

    private String mangleUrl(String url) {
        for (final String search : urlSubstitutions.keySet()) {
            final String replace = urlSubstitutions.get(search);
            url = url.replaceAll(search, replace);
        }

        return url;
    }
}
