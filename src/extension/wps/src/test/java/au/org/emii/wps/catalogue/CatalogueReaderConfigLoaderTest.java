package au.org.emii.wps.catalogue;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class CatalogueReaderConfigLoaderTest {
    @Test
    public void loadCatalogueConfig() throws Exception {
        File configFile = new File(this.getClass().getResource("catalogue.xml").getFile());
        CatalogueReaderConfig config = CatalogueReaderConfigLoader.load(configFile);
        assertEquals("https://catalogue-imos.aodn.org.au/geonetwork", config.getCatalogueUrl());
        assertEquals("layer", config.getLayerSearchField());
    }
}
