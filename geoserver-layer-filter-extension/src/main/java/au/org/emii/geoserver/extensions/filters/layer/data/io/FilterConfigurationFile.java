/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FilterConfigurationFile extends FilterConfigurationIO {

    private File file;
    private FilterConfigurationReader filterConfigurationReader;

    public FilterConfigurationFile(String dataDirectoryPath) {
        this.dataDirectoryPath = dataDirectoryPath;
    }

    public List<Filter> getFilters() throws ParserConfigurationException, SAXException, IOException {
        if (getFile().exists()) {
            return readFilters(getFile());
        }

        return new ArrayList<Filter>();
    }

    public void write(List<Filter> filters) throws TemplateException, IOException {
        FilterConfigurationWriter configurationWriter = new FilterConfigurationWriter(filters);

        Writer writer = null;
        try {
            writer = new FileWriter(getFilePath());
            configurationWriter.write(writer);
        }
        finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private List<Filter> readFilters(File file) throws ParserConfigurationException, SAXException, IOException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            return getReader().read(fileInputStream).getFilters();
        }
        finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    private File getFile() {
        if (file == null) {
            file = new File(getFilePath());
        }
        return file;
    }

    private FilterConfigurationReader getReader() {
        if (filterConfigurationReader == null) {
            filterConfigurationReader = new FilterConfigurationReader(dataDirectoryPath);
        }
        return filterConfigurationReader;
    }

    private String getFilePath() {
        return String.format("%s/%s", dataDirectoryPath, FILTER_CONFIGURATION_FILE_NAME);
    }
}
