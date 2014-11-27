/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import au.org.emii.geoserver.extensions.filters.layer.data.FilterConfiguration;
import au.org.emii.geoserver.extensions.filters.layer.data.io.LayerPropertiesReader;
import au.org.emii.geoserver.extensions.filters.layer.data.io.LayerPropertiesReaderFactory;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jndi.JndiTemplate;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayerFilterConfigurationPage extends GeoServerSecuredPage {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters");

    public static final String NAME = "name";

    private String layerName;
    private String storeName;
    private String workspaceName;

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
            LOGGER.log(Level.SEVERE, "Error getting DataSource from JNDI reference", e);
        }
        catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Error getting DataSource from JNDI reference", ioe);
        }
    }

    @Override
    protected String getTitle() {
        return String.format("%s - %s - %s", workspaceName, storeName, layerName);
    }

    @Override
    protected String getDescription() {
        return String.format("Configuring filters for %s - %s - %s", workspaceName, storeName, layerName);
    }

    public void setContext(ServletContext context) {
        // TODO dig into this and see what is happening
        LOGGER.log(Level.WARNING, "Setting the context");
        this.context = context;
    }

    private LayerFilterForm getLayerFilterForm() throws NamingException, IOException {
        return new LayerFilterForm("layerFilterForm", getFilterConfigurationModel());
    }

    private DataSource getDataSource() throws NamingException {
        JndiTemplate template = new JndiTemplate();
        return (DataSource)template.lookup(getDataStoreParameter("jndiReferenceName"));
    }

    private DataStoreInfo getDataStoreInfo() {
        return getCatalog().getDataStoreByName(workspaceName, storeName);
    }

    private String getDataStoreParameter(String parameter) {
        return (String)getDataStoreInfo().getConnectionParameters().get(parameter);
    }

    private IModel<FilterConfiguration> getFilterConfigurationModel() throws NamingException, IOException {
        LayerPropertiesReader reader = LayerPropertiesReaderFactory.getReader(getDataSource(), layerName, getDataStoreParameter("schema"));

        String dataDir = GeoServerResourceLoader.lookupGeoServerDataDirectory(context);

        //GeoServerDataDirectory dataDirectory = new GeoServerDataDirectory(getCatalog().getResourceLoader());
        //File dir = dataDirectory.findDataDir(workspaceName, storeName, layerName);

        String dir = Paths.path(dataDir, "workspaces", workspaceName, storeName, layerName);

        LOGGER.log(Level.WARNING, String.format("data dir? %s", dir));

        final FilterConfiguration config = new FilterConfiguration(dir, reader.read());

        return new Model<FilterConfiguration>() {
            @Override
            public FilterConfiguration getObject() {
                return config;
            }
        };
    }
}
