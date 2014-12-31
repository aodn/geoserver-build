/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.wfs.response;

import au.org.emii.geoserver.extensions.filters.layer.data.DataDirectory;
import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import au.org.emii.geoserver.extensions.filters.layer.data.io.FilterConfigurationFile;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.response.CSVOutputFormat;
import org.geotools.feature.FeatureCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RestrictedColumnCsvOutputFormat extends WFSGetFeatureOutputFormat {

    private Catalog catalog;
    private CSVOutputFormat csvOutputFormat;

    @Autowired
    private ServletContext context;

    public RestrictedColumnCsvOutputFormat(GeoServer geoserver, Catalog catalog, CSVOutputFormat csvOutputFormat) {
        super(geoserver, "csv-restricted-column");
        this.catalog = catalog;
        this.csvOutputFormat = csvOutputFormat;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return csvOutputFormat.getMimeType(value, operation);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return csvOutputFormat.getPreferredDisposition(value, operation);
    }

    @Override
    public String getCapabilitiesElementName() {
        return csvOutputFormat.getCapabilitiesElementName();
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return csvOutputFormat.getAttachmentFileName(value, operation);
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollectionResponse, OutputStream output, Operation getFeature)
        throws IOException, ServiceException
    {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
        FilterConfigurationFile file = new FilterConfigurationFile(getDataDirectory(getFeature));
        try {
            Set<String> excludedFilters = getExcludedFilters(file.getFilters());
            FeatureCollection<?, ?> featureCollection = (FeatureCollection<?, ?>)featureCollectionResponse.getFeature().get(0);

            RestrictedColumnCsvOutputFormatHeader header = new RestrictedColumnCsvOutputFormatHeader(excludedFilters, getCoordFormatter());
            header.write(featureCollection, writer);

            RestrictedColumnCsvOutputFormatLines lines = new RestrictedColumnCsvOutputFormatLines(excludedFilters, getCoordFormatter());
            lines.write(featureCollection, writer);
        }
        catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);
        }
        catch (SAXException se) {
            throw new RuntimeException(se);
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private String getDataDirectory(Operation operation) {
        return new DataDirectory(context).getLayerDataDirectoryPath(catalog, operation);
    }

    private Set<String> getExcludedFilters(List<Filter> filters) {
        Set<String> excluded = new HashSet<String>();
        for (Filter filter : filters) {
            if (filter.isExcludedFromDownload() != null && filter.isExcludedFromDownload().booleanValue()) {
                excluded.add(filter.getName());
            }
        }
        return excluded;
    }

    private NumberFormat getCoordFormatter() {
        NumberFormat coordFormatter = NumberFormat.getInstance(Locale.US);
        coordFormatter.setMaximumFractionDigits(getInfo().getGeoServer().getSettings().getNumDecimals());
        coordFormatter.setGroupingUsed(false);

        return coordFormatter;
    }
}
