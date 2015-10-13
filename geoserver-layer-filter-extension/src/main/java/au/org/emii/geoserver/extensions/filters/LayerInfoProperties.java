/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.feature.NameImpl;

import java.util.Arrays;
import java.util.List;


public class LayerInfoProperties {

    static final GeoServerDataProvider.Property<LayerInfo> WORKSPACE = new GeoServerDataProvider.BeanProperty<LayerInfo>("workspace", "resource.store.workspace.name");

    static final GeoServerDataProvider.Property<LayerInfo> STORE = new GeoServerDataProvider.BeanProperty<LayerInfo>("store", "resource.store.name");

    static final GeoServerDataProvider.Property<LayerInfo> NAME = new GeoServerDataProvider.BeanProperty<LayerInfo>("name", "name");

    static final List<GeoServerDataProvider.Property<LayerInfo>> PROPERTIES = Arrays.asList(WORKSPACE, STORE, NAME);

    public static LayerInfo getLayer(Catalog catalog, String workspaceName, String layerName) {
        // Pulled straight from the Geoserver source for ResourceConfigurationPage
        if (workspaceName != null) {
            NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);
            if (ns == null) {
                throw new RuntimeException("Could not find workspace " + workspaceName);
            }
            String nsURI = ns.getURI();
            return catalog.getLayerByName(new NameImpl(nsURI, layerName));
        }

        return catalog.getLayerByName(layerName);
    }

}
