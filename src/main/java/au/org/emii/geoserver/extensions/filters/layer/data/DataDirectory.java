/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;

import javax.servlet.ServletContext;
import java.io.File;

public class DataDirectory {

    private ServletContext context;

    public DataDirectory(ServletContext context) {
        this.context = context;
    }

    public String getLayerDataDirectoryPath(LayerInfo layerInfo) {
        GeoServerDataDirectory dataDirectory = new GeoServerDataDirectory(new File(getGeoServerDataDirectory()));
        return dataDirectory.get(layerInfo).dir().getAbsolutePath();
    }

    private String getGeoServerDataDirectory() {
        return GeoServerResourceLoader.lookupGeoServerDataDirectory(context);
    }

}
