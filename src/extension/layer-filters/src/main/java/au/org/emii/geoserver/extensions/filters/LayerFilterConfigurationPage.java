/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import au.org.emii.geoserver.extensions.filters.layer.data.DataDirectory;
import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import au.org.emii.geoserver.extensions.filters.layer.data.FilterConfiguration;
import au.org.emii.geoserver.extensions.filters.layer.data.FilterMerge;
import au.org.emii.geoserver.extensions.filters.layer.data.io.FilterConfigurationFile;
import au.org.emii.geoserver.extensions.filters.layer.data.io.LayerPropertiesReader;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayerFilterConfigurationPage extends GeoServerSecuredPage {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters");

    public static final String NAME = "name";

    private String layerName;
    private String storeName;
    private String workspaceName;
    private String dataDirectory;

    @Autowired
    private ServletContext context;

    public LayerFilterConfigurationPage(PageParameters parameters) {
        this(
            parameters.getString(DataAccessEditPage.WS_NAME),
            parameters.getString(DataAccessEditPage.STORE_NAME),
            parameters.getString(NAME)
        );
    }

    public LayerFilterConfigurationPage(String workspaceName, String storeName, String layerName) {
        this.workspaceName = workspaceName;
        this.storeName = storeName;
        this.layerName = layerName;

        try {
            add(getLayerFilterForm());
            add(CSSPackageResource.getHeaderContribution(LayerFilterConfigurationPage.class, "layer_filters.css"));
        }
        catch (NamingException e) {
            throw new FilterConfigurationException("Error getting DataSource from JNDI reference", e);
        }
        catch (ParserConfigurationException pce) {
            throw new FilterConfigurationException("Could not parse saved filter configuration", pce);
        }
        catch (SAXException se) {
            throw new FilterConfigurationException("Could not parse saved filter configuration", se);
        }
        catch (IOException ioe) {
            throw new FilterConfigurationException("Error reading filters", ioe);
        }
    }

    @Override
    protected String getTitle() {
        return String.format("%s/%s/%s", workspaceName, storeName, layerName);
    }

    @Override
    protected String getDescription() {
        return String.format("Configuring filters for %s/%s/%s", workspaceName, storeName, layerName);
    }

    private LayerFilterForm getLayerFilterForm() throws NamingException, ParserConfigurationException, SAXException, IOException {
        return new LayerFilterForm("layerFilterForm", getFilterConfigurationModel());
    }

    private List<Filter> getLayerProperties() throws NamingException, IOException {
        return new LayerPropertiesReader(getCatalog(), LayerInfoProperties.getLayer(getCatalog(), workspaceName, layerName)).read();
    }

    private List<Filter> getConfiguredFilters() throws ParserConfigurationException, SAXException, IOException {
        return new FilterConfigurationFile(getDataDirectory()).getFilters();
    }

    private IModel<FilterConfiguration> getFilterConfigurationModel() throws NamingException, ParserConfigurationException, SAXException, IOException {

        // we want configured filters on the left
        final FilterConfiguration config = new FilterConfiguration(getDataDirectory(), FilterMerge.merge(getLayerProperties(), getConfiguredFilters()));

        return new Model<FilterConfiguration>() {
            @Override
            public FilterConfiguration getObject() {
                return config;
            }
        };
    }

    private String getDataDirectory() {
        if (dataDirectory == null) {
            dataDirectory = new DataDirectory(context).getLayerDataDirectoryPath(
                LayerInfoProperties.getLayer(getCatalog(), workspaceName, layerName)
            );
        }

        return dataDirectory;
    }
}
