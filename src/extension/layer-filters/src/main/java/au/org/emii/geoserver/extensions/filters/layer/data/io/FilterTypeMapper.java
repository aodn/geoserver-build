/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import org.geoserver.wfs.xml.GML2Profile;
import org.geoserver.wfs.xml.TypeMappingProfile;
import org.geoserver.wfs.xml.XSProfile;
import org.opengis.feature.type.AttributeType;

/**
 * Hides some ugly type mapping code in to this one class.
 */
public class FilterTypeMapper {

    public String getTypeForClass(Class attributeClass) {
        AttributeType attrType = getXsdAttributeTypeForClass(attributeClass);
        if (attrType == null) {
            attrType = attrType = getGml2AttributeTypeForClass(attributeClass);
        }

        return attrType.getName().getLocalPart().toLowerCase();
    }

    private AttributeType getXsdAttributeTypeForClass(Class attributeClass) {
        TypeMappingProfile xsProfile = new XSProfile();
        return xsProfile.type(attributeClass);
    }

    private AttributeType getGml2AttributeTypeForClass(Class attributeClass) {
        TypeMappingProfile gml2Profile = new GML2Profile();
        return gml2Profile.type(attributeClass);
    }
}
