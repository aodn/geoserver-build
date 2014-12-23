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
import au.org.emii.geoserver.extensions.filters.layer.data.io.LayerDataStore;
import au.org.emii.geoserver.extensions.filters.layer.data.io.PossibleValuesReader;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.util.*;

public class LayerFiltersService {

    private Catalog catalog;

    @Autowired
    private ServletContext context;

    public LayerFiltersService() {}

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void enabledFilters(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String workspace = request.getParameter("workspace");
        String layer = request.getParameter("layer");

        try {
            respondWithDocument(response, getDocument(workspace, layer));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void respondWithDocument(HttpServletResponse response, Document document) throws TransformerException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(response.getOutputStream());
        transformer.transform(source, result);
    }

    private Document getDocument(String workspace, String layer)
        throws ParserConfigurationException, SAXException, IOException, NamingException
    {
        LayerInfo layerInfo = getLayerInfo(workspace, layer);
        FilterConfigurationFile file = new FilterConfigurationFile(getLayerDataDirectoryPath(layerInfo));
        List<Filter> filters = file.getFilters();
        new PossibleValuesReader().read(layerInfo, filters);

        return new FiltersDocument().build(filters);
    }



    private StoreInfo getStoreInfo(LayerInfo layerInfo) {
        return layerInfo.getResource().getStore();
    }

    private String getStoreName(LayerInfo layerInfo) {
        return getStoreInfo(layerInfo).getName();
    }

    private LayerInfo getLayerInfo(String workspace, String layer) {
        return LayerInfoProperties.getLayer(getCatalog(), workspace, layer);
    }

    private String getLayerDataDirectoryPath(LayerInfo layerInfo) {
        return new DataDirectory(context).getLayerDataDirectoryPath(layerInfo);
    }

    private DataSource getDataSource(String workspaceName, String storeName) throws NamingException {
        return getLayerDataStore(workspaceName, storeName).getDataSource();
    }

    private String getSchemaName(String workspaceName, String storeName) {
        return getLayerDataStore(workspaceName, storeName).getDataStoreParameter("schema");
    }

    private LayerDataStore getLayerDataStore(String workspaceName, String storeName) {
        return new LayerDataStore(getCatalog(), workspaceName, storeName);
    }
}
