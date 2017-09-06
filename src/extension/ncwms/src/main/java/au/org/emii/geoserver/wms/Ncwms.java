package au.org.emii.geoserver.wms;

import org.apache.commons.io.IOUtils;
import org.geotools.util.logging.Logging;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Ncwms {
    static Logger LOGGER = Logging.getLogger(Ncwms.class);

    /* Sample tiny config file:
       <ncwms>
         <wfsServer>http://localhost:8080/geoserver/ows</wfsServer>
         <urlSubstitution key="/mnt/imos-t3/IMOS/opendap/">http://thredds.aodn.org.au/thredds/wms/IMOS/</urlSubstitution>
         <urlSubstitution key="^/IMOS/">http://thredds.aodn.org.au/thredds/wms/IMOS/</urlSubstitution>
         <collectionsWithTimeMismatch>^imos:srs.*</collectionsWithTimeMismatch>
       </ncwms>
    */

    private final Map<String, String> urlSubstitutions;
    private final List<String> collectionsWithTimeMismatchRegExs;

    private final UriIndex geoserverUrlIndex;

    public Ncwms(UriIndex geoserverUrlIndex, NcwmsConfig ncwmsConfig) {
        this.geoserverUrlIndex = geoserverUrlIndex;
        urlSubstitutions = ncwmsConfig.getConfigMap("/ncwms/urlSubstitution");
        collectionsWithTimeMismatchRegExs = ncwmsConfig.getConfigList("/ncwms/collectionsWithTimeMismatch");

        for (Map.Entry<String, String> entry : urlSubstitutions.entrySet()) {
            LOGGER.log(Level.INFO, String.format("urlSubstitution: '%s' -> '%s'", entry.getKey(), entry.getValue()));
        }
    }

    public void getMetadata(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "GetMetadata");
        final LayerDescriptor layerDescriptor = new LayerDescriptor(request.getParameter("layerName"));
        final String item = request.getParameter("item");

        if (item != null && item.compareTo("timesteps") == 0) {
            String day = request.getParameter("day");

            JSONObject resultJson = new JSONObject();
            resultJson.put("timesteps", geoserverUrlIndex.getTimesForDay(layerDescriptor, day));
            response.getOutputStream().write(resultJson.toString().getBytes());
        } else if (item != null && item.compareTo("layerDetails") == 0) {

            JSONObject getMetadataJson = getMetadataJson(layerDescriptor);
            getMetadataJson.put("datesWithData", geoserverUrlIndex.getUniqueDates(layerDescriptor));
            LOGGER.log(Level.INFO, "Returning getMetadataJson");
            response.getOutputStream().write(getMetadataJson.toString().getBytes());
        }
    }

    public void getMap(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        proxyWmsRequest(request, response);
    }

    public void getLegendGraphic(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        proxyWmsRequest(request, response, "LAYER");
    }

    public void getFeatureInfo(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        proxyWmsRequest(request, response);
    }

    private void proxyWmsRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        proxyWmsRequest(request, response, "LAYERS");
    }

    private void proxyWmsRequest(HttpServletRequest request, HttpServletResponse response, String layerParameter)
            throws ServletException, IOException {
        LayerDescriptor layerDescriptor = new LayerDescriptor(request.getParameter(layerParameter));

        String time = request.getParameter("TIME");

        String wmsUrlStr = getWmsUrl(layerDescriptor, time);

        try {
            @SuppressWarnings("unchecked")
            Map<String, String[]> wmsParameters = new HashMap<String, String[]>(request.getParameterMap());

            // Some collections such as SRS are indexed with a timestamp which doesn't match
            // the timestamp thredds calculates but only have one timestamp per file meaning its
            // not actually required
            // For the moment just don't include the time parameter for these collections
            if (isCollectionWithTimeMismatch(layerDescriptor.geoserverName())) {
                wmsParameters.remove("TIME");
            }

            wmsParameters.put(layerParameter, new String[] { layerDescriptor.getNetCDFVariableName() });

            // Needed for GetFeatureInfo
            if (wmsParameters.containsKey("QUERY_LAYERS")) {
                wmsParameters.put("QUERY_LAYERS", new String[]{layerDescriptor.getNetCDFVariableName() });
            }

            String queryString = encodeMapForRequest(wmsParameters);

            URL wmsUrl = new URL(wmsUrlStr + "?" + queryString);

            HttpURLConnection connection = (HttpURLConnection) wmsUrl.openConnection();
            if (connection.getResponseCode() != 200 ) {
                String ret = String.format("ERROR proxying URL '%s' - %s", wmsUrl, connection.getResponseMessage());
                response.sendError(connection.getResponseCode(), ret);
                LOGGER.log(Level.SEVERE, ret);
            }
            else {
                response.setStatus(connection.getResponseCode());
                response.setContentType(connection.getContentType());
                response.setContentLength(connection.getContentLength());
                try (InputStream is = connection.getInputStream()) {
                    IOUtils.copy(is, response.getOutputStream());
                }
            }
        } catch (Exception e) {
            String ret = String.format("Problem while proxying URL '%s' - %s", wmsUrlStr, e.getMessage());
            response.sendError(500, e.getMessage());
            LOGGER.log(Level.SEVERE, ret);
            e.printStackTrace();
        }
    }

    private boolean isCollectionWithTimeMismatch(String layerName) {
        for (String collectionsWithTimeMismatchRegEx: collectionsWithTimeMismatchRegExs) {
            if (Pattern.matches(collectionsWithTimeMismatchRegEx, layerName)) {
                return true;
            }
        }

        return false;
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error encoding parameters: '%s'", e.getMessage()));
        }

        return sb.toString();
    }

    public JSONObject getMetadataJson(LayerDescriptor layerDescriptor) {
        String wmsUrl;

        try {
            wmsUrl = getWmsUrl(layerDescriptor, null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error retreiving getWmsUrl: '%s'", e.getStackTrace()));
            e.printStackTrace();
            return null;
        }

        String metadataUrl = String.format("%s?service=WMS&version=1.3.0&request=GetMetadata&item=layerDetails&layerName=%s",
                wmsUrl,
                layerDescriptor.getNetCDFVariableName()
        );

        JSONParser parser = new JSONParser();

        try {
            return (JSONObject) parser.parse(readUrl(metadataUrl));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error retreiving metadataUrl: '%s'", e.getMessage()));
            return null;
        }
    }

    private String getWmsUrl(LayerDescriptor layerDescriptor, String time) throws IOException {
        return mangleUrl(geoserverUrlIndex.getUrlForTimestamp(layerDescriptor, time));
    }

    private String mangleUrl(String url) {
        for (final String search : urlSubstitutions.keySet()) {
            final String replace = urlSubstitutions.get(search);
            url = url.replaceAll(search, replace);
        }
        return url;
    }

    private static String readUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {

            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        }
    }
}
