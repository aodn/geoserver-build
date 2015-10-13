/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.geoserver.catalog.*;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LayerPropertiesReader {

    private Catalog catalog;
    private LayerInfo layerInfo;

    public LayerPropertiesReader(Catalog catalog, LayerInfo layerInfo) {
        this.catalog = catalog;
        this.layerInfo = layerInfo;
    }

    public List<Filter> read() throws IOException {
        FeatureTypeInfo typeInfo = (FeatureTypeInfo)layerInfo.getResource();
        final ResourcePool resourcePool = catalog.getResourcePool();
        final FeatureType featureType = resourcePool.getFeatureType(typeInfo);

        return buildFilters(featureType, resourcePool.getAttributes(typeInfo));
    }

    private List<Filter> buildFilters(FeatureType featureType, List<AttributeTypeInfo> attributes) {
        List<Filter> filters = new ArrayList<Filter>();
        for (AttributeTypeInfo attribute : attributes) {
            filters.add(new Filter(attribute.getName(), getTypeName(featureType, attribute)));
        }

        return filters;
    }

    private PropertyDescriptor getDescriptor(FeatureType featureType, AttributeTypeInfo attribute) {
        return featureType.getDescriptor(attribute.getName());
    }

    private String getTypeName(FeatureType featureType, AttributeTypeInfo attribute) {
        Class attributeClass = getDescriptor(featureType, attribute).getType().getBinding();
        return new FilterTypeMapper().getTypeForClass(attributeClass);
    }
}
