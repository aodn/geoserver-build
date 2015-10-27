/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.data.layer.LayerProvider;

import java.util.List;

public class WmsLayerFilterProvider extends LayerProvider {

    @Override
    protected List<Property<LayerInfo>> getProperties() {
        return LayerInfoProperties.PROPERTIES;
    }

}
