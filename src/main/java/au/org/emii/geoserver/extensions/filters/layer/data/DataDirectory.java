/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import au.org.emii.geoserver.extensions.filters.LayerInfoProperties;
import net.opengis.wfs.QueryType;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geotools.xml.EMFUtils;

import javax.servlet.ServletContext;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;

public class DataDirectory {

    private ServletContext context;

    public DataDirectory(ServletContext context) {
        this.context = context;
    }

    public String getLayerDataDirectoryPath(LayerInfo layerInfo) {
        GeoServerDataDirectory dataDirectory = new GeoServerDataDirectory(new File(getGeoServerDataDirectory()));
        return dataDirectory.get(layerInfo).dir().getAbsolutePath();
    }

    public String getLayerDataDirectoryPath(Catalog catalog, Operation operation) {
        if (hasQuery(operation)) {
            return getLayerDataDirectoryPath(getLayerInfo(catalog, getQuery(operation)));
        }

        return null;
    }

    private String getGeoServerDataDirectory() {
        return GeoServerResourceLoader.lookupGeoServerDataDirectory(context);
    }

    private BasicEList getQueries(Operation operation) {
        return (BasicEList)EMFUtils.get((EObject)operation.getParameters()[0], "query");
    }

    private QName getQualifiedName(QueryType query) {
        return (QName)query.getTypeName().get(0);
    }

    private QueryType getQuery(Operation operation) {
        return (QueryType)getQueries(operation).get(0);
    }

    private LayerInfo getLayerInfo(Catalog catalog, QueryType query) {
        return LayerInfoProperties.getLayer(
            catalog,
            null,
            getQualifiedName(query).getLocalPart()
        );
    }

    private boolean hasQuery(Operation operation) {
        return hasItems(getQueries(operation)) && hasItems(getQuery(operation).getTypeName());
    }

    private boolean hasItems(List items) {
        return items != null && !items.isEmpty();
    }
}
