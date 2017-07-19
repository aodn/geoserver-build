/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import au.org.emii.geoserver.extensions.filters.layer.data.DataDirectory;
import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import au.org.emii.geoserver.extensions.filters.layer.data.FiltersDocument;
import au.org.emii.geoserver.extensions.filters.layer.data.io.FilterConfigurationFile;
import au.org.emii.geoserver.extensions.filters.layer.data.io.PossibleValuesReader;
import au.org.emii.geoserver.extensions.filters.layer.data.ValuesDocument;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LayerFiltersService {
    static final Logger logger = LoggerFactory.getLogger(LayerFiltersService.class);

    public final static String CONFIG_FILE = "filters.xml";

    private GeoServerResourceLoader resourceLoader;

    private Catalog catalog;
    private List<String> uniqueValuesAllowedRegex = null;

    @Autowired
    private ServletContext context;

    public LayerFiltersService() {}

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public List<String> getUniqueValuesAllowedRegex() {
        if (uniqueValuesAllowedRegex != null) {
            return uniqueValuesAllowedRegex;
        }

        try {
            uniqueValuesAllowedRegex = new ArrayList<>();

            File configFile = new File(FilenameUtils.concat(getResourceLoader().getBaseDirectory().toString(), CONFIG_FILE));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(configFile);
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xp = xpathFactory.newXPath();

            XPathExpression expr = xp.compile("/filters/allowed/regex");
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            for(int i = 0; i < nl.getLength() ; i++) {
                String regex = nl.item(i).getTextContent();
                uniqueValuesAllowedRegex.add(regex);
                logger.warn(String.format("Allowing uniqueValues regex '%s'", regex));
            }
        }
        catch (Exception e) {
            logger.error(String.format("Could not parse allowed regex from config file '%s': '%s'", CONFIG_FILE, e.getMessage()));
            logger.warn("Allowing uniqueValues on any configured layer.");
        }

        return uniqueValuesAllowedRegex;
    }

    private boolean uniqueValuesAllowed(String workspace, String layer, String propertyName) {
        if (getUniqueValuesAllowedRegex().isEmpty())
            return true;

        String fullLayerName = String.format("%s:%s/%s", workspace, layer, propertyName);
        for (final String regex : getUniqueValuesAllowedRegex()) {
            if (fullLayerName.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    public void enabledFilters(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String workspace = request.getParameter("workspace");
        String layer = request.getParameter("layer");

        if(workspace==null) {
            String[] splitLayerArray = splitLayerName(layer);
            workspace = splitLayerArray[0];
            layer = splitLayerArray[1];
        }

        try {
            respondWithDocument(response, getEnabledFiltersDocument(workspace, layer));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void uniqueValues(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String workspace = request.getParameter("workspace");
        String layer = request.getParameter("layer");
        String propertyName = request.getParameter("propertyName");

        if(workspace==null) {
            String[] splitLayerArray = splitLayerName(layer);
            workspace = splitLayerArray[0];
            layer = splitLayerArray[1];
        }

        if (! uniqueValuesAllowed(workspace, layer, propertyName)) {
            throw new RuntimeException(String.format("uniqueValues not allowed for '%s:%s/%s'",
                workspace, layer, propertyName));
        }

        try {
            respondWithDocument(response, getUniqueValuesDocument(workspace, layer, propertyName));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] splitLayerName (String layerName) {
        String[] splitArray = new String[2];

        if(layerName.contains(":") ) {
            splitArray = layerName.split(":", 2);
        } else {
            splitArray = new String[] {null,layerName};
        }

        return splitArray;
    }

    private Document getEnabledFiltersDocument(String workspace, String layer)
        throws ParserConfigurationException, SAXException, IOException, NamingException, ServiceException
    {
        LayerInfo layerInfo = getLayerInfo(workspace, layer);

        if (layerInfo == null) {
            throw new ServiceException("Could not find layer " + workspace + ":" + layer);
        }

        FilterConfigurationFile file = new FilterConfigurationFile(getLayerDataDirectoryPath(layerInfo));
        List<Filter> filters = file.getFilters();

        return new FiltersDocument().build(filters);
    }

    private Document getUniqueValuesDocument(String workspace, String layer, String propertyName)
        throws Exception
    {
        LayerInfo layerInfo = getLayerInfo(workspace, layer);
        PossibleValuesReader possibleValuesReader = new PossibleValuesReader();
        Set values = possibleValuesReader.read(getDataStoreInfo(workspace, layer), layerInfo, propertyName);

        return new ValuesDocument().build(values);
    }

    private void respondWithDocument(HttpServletResponse response, Document document) throws TransformerException, IOException {
        response.addHeader("Content-Type", "text/xml");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(response.getOutputStream());
        transformer.transform(source, result);
    }

    private LayerInfo getLayerInfo(String workspace, String layer) {
        return LayerInfoProperties.getLayer(getCatalog(), workspace, layer);
    }

    private DataStoreInfo getDataStoreInfo(String workspace, String layer) {
        return getCatalog().getDataStoreByName(workspace, getLayerInfo(workspace, layer).getResource().getStore().getName());
    }

    private String getLayerDataDirectoryPath(LayerInfo layerInfo) {
        return new DataDirectory(context).getLayerDataDirectoryPath(layerInfo);
    }
}
