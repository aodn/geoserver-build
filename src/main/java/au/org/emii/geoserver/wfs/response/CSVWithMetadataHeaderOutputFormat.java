/*
 * Copyright 2013 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */
package au.org.emii.geoserver.wfs.response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.response.CSVOutputFormat;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;

public class CSVWithMetadataHeaderOutputFormat extends WFSGetFeatureOutputFormat {

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("au.org.emii.geoserver.wfs.response");

    private Catalog catalog;
    private CSVOutputFormat csvOutputFormat;

    public CSVWithMetadataHeaderOutputFormat(GeoServer gs, Catalog catalog, CSVOutputFormat csvOutputFormat) {
        super(gs, "csv-with-metadata-header");
        this.catalog = catalog;
        this.csvOutputFormat = csvOutputFormat;
    }

    @Override
    public String getMimeType(Object value, Operation operation) {
        return this.csvOutputFormat.getMimeType(value, operation);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return this.csvOutputFormat.getPreferredDisposition(value, operation);
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return this.csvOutputFormat.getAttachmentFileName(value, operation);
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection,
                         OutputStream output,
                         Operation getFeature) throws IOException, ServiceException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(output));
        writeMetadata(getMetadataFeatureName(featureCollection), w);
        this.csvOutputFormat.write(featureCollection, output, getFeature);
    }

    private void writeMetadata(String metadataFeatureName,
            BufferedWriter w) throws IOException {
        FeatureTypeInfo ftInfo = catalog.getFeatureTypeByName(metadataFeatureName);
        
        if (ftInfo == null) return;
        
        FeatureSource<? extends FeatureType, ? extends Feature> fs = ftInfo.getFeatureSource(null, null);
        SimpleFeatureCollection collection = (SimpleFeatureCollection) fs.getFeatures();
        
        SimpleFeatureIterator fi = collection.features();
        
        w.write("parameter,units\r\n");
        
        while (fi.hasNext()) {
            SimpleFeature sf = fi.next();
            w.write(sf.getAttribute("parameter")+","+sf.getAttribute("units")+"\r\n");
        }

        w.write("\r\n");
        
        w.flush();
    }

    private String getMetadataFeatureName(
            FeatureCollectionResponse featureCollection) {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection.getFeature().get(0);
        return fc.getSchema().getName() + "_metadata";
    }

    @Override
    public String getCapabilitiesElementName() {
        return this.csvOutputFormat.getCapabilitiesElementName();
    }
}
