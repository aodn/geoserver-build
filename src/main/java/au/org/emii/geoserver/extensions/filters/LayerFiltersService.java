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
import au.org.emii.geoserver.extensions.filters.layer.data.io.PossibleValuesReaderFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geotools.feature.NameImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

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
        Document document = null;
        LayerInfo layerInfo = getLayerInfo(workspace, layer);

        FilterConfigurationFile file = new FilterConfigurationFile(
            getLayerDataDirectoryPath(workspace, layer, layerInfo)
        );

        try {
            document = getNewDocument();

            List<Filter> filters = file.getFilters();
            PossibleValuesReaderFactory.getReader(
                getDataSource(workspace, getStoreName(layerInfo)),
                layer,
                getSchemaName(workspace, getStoreName(layerInfo))
            ).read(filters);

            new FiltersDocument().build(document, filters);
        }
        catch (FileNotFoundException fnfe) {
            document = getEmptyDocument();
        }

        return document;
    }

    private Document getNewDocument() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        return docBuilder.newDocument();
    }

    private Document getEmptyDocument() throws ParserConfigurationException {
        Document document = getNewDocument();
        document.appendChild(document.createElement("filters"));

        return document;
    }

    private StoreInfo getStoreInfo(LayerInfo layerInfo) {
        return layerInfo.getResource().getStore();
    }

    private String getStoreName(LayerInfo layerInfo) {
        return getStoreInfo(layerInfo).getName();
    }

    private LayerInfo getLayerInfo(String workspace, String layer) {
        // Pulled straight from the Geoserver source for ResourceConfigurationPage
        if (workspace != null) {
            NamespaceInfo ns = getCatalog().getNamespaceByPrefix(workspace);
            if (ns == null) {
                throw new RuntimeException("Could not find workspace " + workspace);
            }
            String nsURI = ns.getURI();
            return getCatalog().getLayerByName(new NameImpl(nsURI, layer));
        }

        return getCatalog().getLayerByName(layer);
    }

    private String getLayerDataDirectoryPath(String workspace, String layer, LayerInfo layerInfo) {
        return new DataDirectory(context).getLayerDataDirectoryPath(
            workspace,
            getStoreInfo(layerInfo).getName(),
            layer
        );
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
