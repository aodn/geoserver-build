/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import au.org.emii.geoserver.extensions.filters.layer.data.LayerDataAccessor;
import au.org.emii.geoserver.extensions.filters.layer.data.LayerIdentifier;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geotools.util.logging.Logging;
import org.springframework.jndi.JndiTemplate;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayerFilterConfigurationPage extends GeoServerSecuredPage {

    static Logger LOGGER = Logging.getLogger("au.org.emii.geoserver.extensions.filters");

    public static final String NAME = "name";

    private String layerName;
    private String storeName;
    private String workspaceName;

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
            LOGGER.log(Level.SEVERE, "it failed tommy", e);
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

    private LayerFilterForm getLayerFilterForm() throws NamingException {
        return new LayerFilterForm("layerFilterForm", new LayerDataAccessor(getDataSource(), new LayerIdentifier(layerName, getDataStoreParameter("schema"))));
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
}
