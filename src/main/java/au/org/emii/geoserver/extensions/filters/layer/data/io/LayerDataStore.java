/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.springframework.jndi.JndiTemplate;

import javax.naming.NamingException;
import javax.sql.DataSource;

public class LayerDataStore {

    private Catalog catalog;
    private String workspaceName;
    private String storeName;

    public LayerDataStore(Catalog catalog, String workspaceName, String storeName) {
        this.catalog = catalog;
        this.workspaceName = workspaceName;
        this.storeName = storeName;
    }

    public DataSource getDataSource() throws NamingException {
        JndiTemplate template = new JndiTemplate();
        return (DataSource)template.lookup(getDataStoreParameter("jndiReferenceName"));
    }

    public DataStoreInfo getDataStoreInfo() {
        return catalog.getDataStoreByName(workspaceName, storeName);
    }

    public String getDataStoreParameter(String parameter) {
        return (String)getDataStoreInfo().getConnectionParameters().get(parameter);
    }
}
