package au.org.emii.geoserver.wms;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultText;
import org.dom4j.xpath.DefaultXPath;
import org.geotools.util.logging.Logging;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.net.URL;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Ncwms {
    static Logger LOGGER = Logging.getLogger(Ncwms.class);

    public static String wmsVersion = "1.3.0";

    /* Sample tiny config file:
       <ncwms>
         <wfsServer>http://localhost:8080/geoserver/ows</wfsServer>
         <urlSubstitution key="/mnt/imos-t3/IMOS/opendap/">http://thredds-1-aws-syd.aodn.org.au/thredds/wms/IMOS/</urlSubstitution>
         <urlSubstitution key="^/IMOS/">http://thredds-1-aws-syd.aodn.org.au/thredds/wms/IMOS/</urlSubstitution>
         <collectionsWithTimeMismatch>^imos:srs.*</collectionsWithTimeMismatch>
       </ncwms>
    */

    private final Map<String, String> urlSubstitutions;
    private final List<String> collectionsWithTimeMismatchRegExs;

    private final UrlIndexInterface urlIndexInterface;

    public Ncwms(UrlIndexInterface urlIndexInterface, NcwmsConfig ncwmsConfig) {
        this.urlIndexInterface = urlIndexInterface;
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
            resultJson.put("timesteps", urlIndexInterface.getTimesForDay(layerDescriptor, day));
            response.getOutputStream().write(resultJson.toString().getBytes());
        }
        else if (item != null && item.compareTo("layerDetails") == 0) {
            LOGGER.log(Level.INFO, "Returning all available dates");

            Document getCapabilitiesDocument = getCapabilitiesXml(layerDescriptor);

            JSONObject resultJson = new JSONObject();
            resultJson.put("datesWithData", urlIndexInterface.getUniqueDates(layerDescriptor));
            resultJson.put("supportedStyles", getSupportedStyles(getCapabilitiesDocument, layerDescriptor.variable));
            resultJson.put("palettes", getPalettes(getCapabilitiesDocument, layerDescriptor.variable));
            response.getOutputStream().write(resultJson.toString().getBytes());
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

            wmsParameters.put("VERSION", new String[] { wmsVersion });
            wmsParameters.put(layerParameter, new String[] { layerDescriptor.variable });

            // Needed for GetFeatureInfo
            if (wmsParameters.containsKey("QUERY_LAYERS")) {
                wmsParameters.put("QUERY_LAYERS", new String[] { layerDescriptor.variable });
            }

            String queryString = encodeMapForRequest(wmsParameters);

            URL wmsUrl = new URL(wmsUrlStr + "?" + queryString);

            IOUtils.copy(wmsUrl.openConnection().getInputStream(), response.getOutputStream());
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Problem proxying url '%s'", wmsUrlStr));
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
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error encoding parameters: '%s'", e.getMessage()));
        }

        return sb.toString();
    }

    private static List<String> getCombinedStyles(Document getCapabilitiesXml, String layerName) {
        List<String> combinedStyles = new ArrayList<>();

        DefaultXPath xpath = new DefaultXPath("//x:Layer/x:Title[.=\'" + layerName + "\']/../x:Style/x:Name/text()");
        Map<String,String> namespaces = new TreeMap<>();
        namespaces.put("x", "http://www.opengis.net/wms");
        xpath.setNamespaceURIs(namespaces);

        @SuppressWarnings("unchecked")
        List<DefaultText> list = xpath.selectNodes(getCapabilitiesXml);

        for (final DefaultText text : list) {
            combinedStyles.add(text.getText());
        }
        return combinedStyles;
    }

    public static List<String> getSupportedStyles(Document getCapabilitiesXml, String layerName) {
        Set<String> styles = new HashSet<>();
        for (final String combinedStyle : getCombinedStyles(getCapabilitiesXml, layerName)) {
            styles.add(combinedStyle.split("/")[0]);
        }
        return new ArrayList<>(styles);
    }

    public static List<String> getPalettes(Document getCapabilitiesXml, String layerName) {
        Set<String> palettes = new HashSet<>();
        for (final String combinedStyle : getCombinedStyles(getCapabilitiesXml, layerName)) {
            palettes.add(combinedStyle.split("/")[1]);
        }
        return new ArrayList<>(palettes);
    }

    private Document getCapabilitiesXml(LayerDescriptor layerDescriptor)
        throws IOException {
        String wmsUrl = null;
        try {
            wmsUrl = getWmsUrl(layerDescriptor, null);
            String getCapabilitiesUrl =
                String.format("%s?service=WMS&version=1.3.0&request=GetCapabilities",
                    wmsUrl
                );

            SAXReader reader = new SAXReader();
            return reader.read(new URL(getCapabilitiesUrl));
        }
        catch (DocumentException e) {
            LOGGER.log(Level.SEVERE, String.format("Error parsing GetCapabilities XML document at '%s'", wmsUrl));
            throw new IOException(e);
        }
    }

    private String getWmsUrl(LayerDescriptor layerDescriptor, String time) throws IOException {
        return mangleUrl(urlIndexInterface.getUrlForTimestamp(layerDescriptor, time));
    }

    private String mangleUrl(String url) {
        for (final String search : urlSubstitutions.keySet()) {
            final String replace = urlSubstitutions.get(search);
            url = url.replaceAll(search, replace);
        }

        return url;
    }
}
