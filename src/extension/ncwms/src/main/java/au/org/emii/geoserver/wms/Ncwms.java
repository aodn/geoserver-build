package au.org.emii.geoserver.wms;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultText;
import org.dom4j.xpath.DefaultXPath;
import org.geotools.util.logging.Logging;
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
    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.wms.ncwms");

    public static String wmsVersion = "1.3.0";

    public static Map<String, String> urlSubstitutions = new HashMap<String, String>();

    private final URLIndexInterface urlIndexInterface;

    public void setUrlSubstitutions(Map<String, String> urlSubstitutions) { Ncwms.urlSubstitutions = urlSubstitutions; }
    public Map<String, String> getUrlSubstitutions() { return urlSubstitutions; }

    public Ncwms(URLIndexInterface urlIndexInterface) {
        this.urlIndexInterface = urlIndexInterface;
    }

    public void getMetadata(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "GetMetadata");
        final LayerDescriptor layerDescriptor = new LayerDescriptor(request.getParameter("layerName")); // TODO needs to be case insensitive
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
        LayerDescriptor layerDescriptor = new LayerDescriptor(request.getParameter(layerParameter)); // TODO needs to be case insensitive

        String time = request.getParameter("TIME");

        String wmsUrlStr = getWmsUrl(layerDescriptor, time);

        try {
            Map<String, String[]> wmsParameters = new HashMap(request.getParameterMap());
            wmsParameters.remove("TIME");
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
        List<String> combinedStyles = new ArrayList<String>();

        DefaultXPath xpath = new DefaultXPath("//x:Layer/x:Title[.=\'" + layerName + "\']/../x:Style/x:Name/text()");
        Map<String,String> namespaces = new TreeMap<String, String>();
        namespaces.put("x", "http://www.opengis.net/wms");
        xpath.setNamespaceURIs(namespaces);
        List<DefaultText> list = xpath.selectNodes(getCapabilitiesXml);

        for (final DefaultText text : list) {
            combinedStyles.add(text.getText());
        }
        return combinedStyles;
    }

    public static List<String> getSupportedStyles(Document getCapabilitiesXml, String layerName) {
        Set<String> styles = new HashSet<String>();
        for (final String combinedStyle : getCombinedStyles(getCapabilitiesXml, layerName)) {
            styles.add(combinedStyle.split("/")[0]);
        }
        return new ArrayList<String>(styles);
    }

    public static List<String> getPalettes(Document getCapabilitiesXml, String layerName) {
        Set<String> palettes = new HashSet<String>();
        for (final String combinedStyle : getCombinedStyles(getCapabilitiesXml, layerName)) {
            palettes.add(combinedStyle.split("/")[1]);
        }
        return new ArrayList<String>(palettes);
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
