package au.org.emii.gogoduck.worker;

import au.org.emii.utils.GoGoDuckConfig;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FileMetadataTest {
    public FileMetadata fileMetadata = null;
    public GoGoDuckConfig goGoDuckConfig = null;

    @Before
    public void beforeEach() {
        goGoDuckConfig = mock(GoGoDuckConfig.class);
        fileMetadata = new FileMetadata("", null, "TIME,1,2;LONGITUDE,2,3", goGoDuckConfig);
    }

    @Test
    public void testCarsWeekly() throws Exception {
        String location = "src/test/resources/CARS2009_Australia_weekly.nc";
        fileMetadata.load(new File(location));
        NcksSubsetParameters ncksSubsetParameters = fileMetadata.getSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("DAY_OF_YEAR"));
        assertTrue(ncksSubsetParameters.containsKey("LATITUDE"));
        assertTrue(ncksSubsetParameters.containsKey("LONGITUDE"));
    }

    @Test
    public void testAcorn() throws Exception {
        String location = "src/test/resources/IMOS_ACORN_V_20090827T163000Z_CBG_FV00_1-hour-avg.nc";
        fileMetadata.load(new File(location));
        NcksSubsetParameters ncksSubsetParameters = fileMetadata.getSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("TIME"));
        assertTrue(ncksSubsetParameters.containsKey("LATITUDE"));
        assertTrue(ncksSubsetParameters.containsKey("LONGITUDE"));
    }

    @Test
    public void testSrs() throws Exception {
        String location = "src/test/resources/20160714152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc";
        fileMetadata.load(new File(location));
        NcksSubsetParameters ncksSubsetParameters = fileMetadata.getSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("time"));
        assertTrue(ncksSubsetParameters.containsKey("lat"));
        assertTrue(ncksSubsetParameters.containsKey("lon"));
    }
}
