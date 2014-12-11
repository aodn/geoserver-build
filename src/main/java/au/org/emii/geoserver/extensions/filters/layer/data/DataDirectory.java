/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;

import javax.servlet.ServletContext;

public class DataDirectory {

    private ServletContext context;

    public DataDirectory(ServletContext context) {
        this.context = context;
    }

    public String getLayerDataDirectoryPath(String workspaceName, String storeName, String layerName) {
        return Paths.path(getGeoServerDataDirectory(), "workspaces", workspaceName, storeName, layerName);
    }

    private String getGeoServerDataDirectory() {
        return GeoServerResourceLoader.lookupGeoServerDataDirectory(context);
    }

}
