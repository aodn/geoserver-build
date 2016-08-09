package au.org.emii.gogoduck.worker;

import au.org.emii.utils.GoGoDuckConfig;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoGoDuckModuleTest {
    public GoGoDuckModule ggdm = null;
    public GoGoDuckConfig goGoDuckConfig = null;

    @Before
    public void beforeEach() {
        goGoDuckConfig = mock(GoGoDuckConfig.class);
        ggdm = new GoGoDuckModule("", null, "TIME,1,2;LONGITUDE,2,3", goGoDuckConfig);
    }

    @Test
    public void testCarsWeekly() throws Exception {
        String location = "src/test/resources/CARS2009_Australia_weekly.nc";
        ggdm.loadFileMetadata(new File(location));
        NcksSubsetParameters ncksSubsetParameters = ggdm.getSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("DAY_OF_YEAR"));
        assertTrue(ncksSubsetParameters.containsKey("LATITUDE"));
        assertTrue(ncksSubsetParameters.containsKey("LONGITUDE"));
    }

    @Test
    public void testAcorn() throws Exception {
        String location = "src/test/resources/IMOS_ACORN_V_20090827T163000Z_CBG_FV00_1-hour-avg.nc";
        ggdm.loadFileMetadata(new File(location));
        NcksSubsetParameters ncksSubsetParameters = ggdm.getSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("TIME"));
        assertTrue(ncksSubsetParameters.containsKey("LATITUDE"));
        assertTrue(ncksSubsetParameters.containsKey("LONGITUDE"));
    }

    @Test
    public void testSrs() throws Exception {
        String location = "src/test/resources/20160714152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc";
        ggdm.loadFileMetadata(new File(location));
        NcksSubsetParameters ncksSubsetParameters = ggdm.getSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("time"));
        assertTrue(ncksSubsetParameters.containsKey("lat"));
        assertTrue(ncksSubsetParameters.containsKey("lon"));
    }
}
