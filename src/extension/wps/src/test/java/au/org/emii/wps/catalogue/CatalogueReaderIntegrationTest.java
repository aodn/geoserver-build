package au.org.emii.wps.catalogue;

import org.junit.Test;

import static org.junit.Assert.*;

public class CatalogueReaderIntegrationTest {
    @Test
    public void getMetadataUrl() throws Exception {
        CatalogueReaderConfig config = new CatalogueReaderConfig("https://catalogue-imos.aodn.org.au/geonetwork", "any");
        CatalogueReader catalogueReader = new CatalogueReader(config);
        String metadataUrl = catalogueReader.getMetadataUrl("argo_profile_map");
        assertEquals("https://catalogue-imos.aodn.org.au:443/geonetwork/srv/en/metadata.show?uuid=4402cb50-e20a-44ee-93e6-4728259250d2", metadataUrl);
    }
}
