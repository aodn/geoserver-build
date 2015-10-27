/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.wfs.response;

import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.impl.XSDElementDeclarationImpl;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Set;

public class RestrictedColumnCsvOutputFormatHeader extends RestrictedColumnCsvOutputFormatSection {

    public RestrictedColumnCsvOutputFormatHeader(Set<String> excludedFilterNames, NumberFormat coordFormatter) {
        super(excludedFilterNames, coordFormatter);
    }

    public void write(FeatureCollection<?, ?> featureCollection, Writer writer) throws IOException {
        if (featureCollection.getSchema() instanceof SimpleFeatureType) {
            writeSimpleType(featureCollection, writer);
        }
        else {
            writeComplexType(featureCollection, writer);
        }
        writeNewLine(writer);
    }

    private void writeSimpleType(FeatureCollection<?, ?> featureCollection, Writer writer) throws IOException {
        SimpleFeatureType featureType = (SimpleFeatureType)featureCollection.getSchema();
        writer.write("FID,");
        for (int i = 0; i < featureType.getAttributeCount(); i++) {
            AttributeDescriptor ad = featureType.getDescriptor(i);
            if (include(ad.getLocalName())) {
                writer.write(prepCSVField(ad.getLocalName()));

                if (i < featureType.getAttributeCount() - 1) {
                    writer.write(",");
                }
            }
        }
    }

    private void writeComplexType(FeatureCollection<?, ?> featureCollection, Writer writer) throws IOException {
        writer.write("gml:id,");

        int i = 0;
        for (PropertyDescriptor descriptor : featureCollection.getSchema().getDescriptors()) {
            if (isNotTemporaryAttribute(descriptor)) {
                if (i > 0) {
                    writer.write(",");
                }
                String elName = descriptor.getName().toString();
                Object xsd = descriptor.getUserData().get(XSDElementDeclaration.class);
                if (xsd != null && xsd instanceof XSDElementDeclarationImpl) {
                    // get the prefixed name if possible
                    // otherwise defaults to the full name with namespace URI
                    XSDElementDeclarationImpl xsdEl = (XSDElementDeclarationImpl)xsd;
                    elName = xsdEl.getQName();
                }

                if (include(elName)) {
                    writer.write(prepCSVField(elName));
                    i++;
                }
            }
        }
    }
}
