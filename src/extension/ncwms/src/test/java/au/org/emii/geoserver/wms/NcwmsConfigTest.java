package au.org.emii.geoserver.wms;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Before;
import org.junit.Test;

public class NcwmsConfigTest {
    private NcwmsConfig config;

    @Before
    public void readConfigFile() {
        config = new NcwmsConfig(new GeoServerResourceLoader(getResourcesDirectory()));
    }

    @Test
    public void testGetConfigList() {
        List<String> expectedList = new ArrayList<String>();
        expectedList.add("^imos:srs.*");

        List<String> configList = config.getConfigList("/ncwms/collectionsWithTimeMismatch");

        assertEquals(expectedList, configList);
    }

    private File getResourcesDirectory() {
        return new File(this.getClass().getResource("/").getFile());
    }

}
