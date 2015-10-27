/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.wfs.response;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Set;

public class RestrictedColumnCsvOutputFormatLines extends RestrictedColumnCsvOutputFormatSection {

    public RestrictedColumnCsvOutputFormatLines(Set<String> excludedFilterNames, NumberFormat coordFormatter) {
        super(excludedFilterNames, coordFormatter);
    }

    public void write(FeatureCollection<?, ?> featureCollection, Writer writer) throws IOException {
        FeatureIterator<?> i = featureCollection.features();
        try {
            while(i.hasNext()) {
                Feature feature = i.next();
                writer.write(prepCSVField(feature.getIdentifier().getID()));
                writer.write(",");
                if (feature instanceof SimpleFeature) {
                    writeSimpleType(feature, writer);
                }
                else {
                    writeComplexType(featureCollection, feature, writer);
                }
                writeNewLine(writer);
            }
        }
        finally {
            i.close();
        }
    }

    private void writeSimpleType(Feature feature, Writer writer) throws IOException {
        SimpleFeature sf = (SimpleFeature)feature;
        for (int j = 0; j < sf.getAttributeCount(); j++) {
            Property property = sf.getProperties().toArray(new Property[sf.getProperties().size()])[j];
            if (property != null) {
                if (include(property.getName().getLocalPart())) {
                    String value = formatToString(property.getValue(), coordFormatter);
                    writer.write(prepCSVField(value));

                    if (j < ((SimpleFeature)feature).getAttributeCount() - 1) {
                        writer.write(",");
                    }
                }
            }
        }
    }

    private void writeComplexType(FeatureCollection<?, ?> featureCollection, Feature feature, Writer writer) throws IOException {
        int j = 0;
        for (PropertyDescriptor descriptor : featureCollection.getSchema().getDescriptors()) {
            if (includePropertyDescriptor(descriptor)) {
                if (j > 0) {
                    writer.write(",");
                }
                j++;
                // Multi valued properties aren't supported, only for SF0 for now
                Collection<Property> values = feature.getProperties(descriptor.getName());
                if (values.size() > 1) {
                    throw new UnsupportedOperationException(
                        "Multi valued properties aren't supported with CSV format!");
                }

                Object att = null;
                if (!values.isEmpty()) {
                    att = values.iterator().next().getValue();
                }

                if (att != null) {
                    String value = formatToString(att, coordFormatter);
                    writer.write(prepCSVField(value));
                }
            }
        }
    }

    private boolean includePropertyDescriptor(PropertyDescriptor descriptor) {
        return isNotTemporaryAttribute(descriptor) && include(descriptor.getName().toString());
    }
}
