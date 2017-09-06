package au.org.emii.wps.catalogue;

import com.thoughtworks.xstream.XStream;
import org.geoserver.platform.GeoServerResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class CatalogueReaderConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueReaderConfigLoader.class);

    private final GeoServerResourceLoader resourceLoader;

    public CatalogueReaderConfigLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public CatalogueReaderConfig load() {
        try {
            File configFile = resourceLoader.find("wps/catalogue.xml");

            if (configFile == null) {
                logger.warn("wps/catalogue.xml not found");
                return null;
            }

            return load(configFile);
        } catch (IOException e) {
            logger.warn("Error loading wps/catalogue.xml", e);
            return null;
        }
    }

    protected static CatalogueReaderConfig load(File location) {
        XStream xStream = new XStream();
        xStream.alias("catalogue", CatalogueReaderConfig.class);
        return (CatalogueReaderConfig) xStream.fromXML(location);
    }
}
