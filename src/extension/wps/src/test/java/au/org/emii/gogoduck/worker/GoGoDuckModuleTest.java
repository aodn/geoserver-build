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
    public void testUpdateMetadata() throws Exception {
        File sampleNetcdf = new File("src/test/resources/file.nc").getAbsoluteFile();
        File tmpFile = File.createTempFile("tmp", ".nc");
        Files.delete(tmpFile.toPath());
        Files.copy(sampleNetcdf.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // preparing test data
        List<Attribute> newAttributeList = new ArrayList<Attribute>();
        newAttributeList.add(new Attribute("test_attribute", "test_value"));

        ggdm = mock(GoGoDuckModule.class);
        when(ggdm.getGlobalAttributesToUpdate(any(NetcdfFile.class))).thenReturn(newAttributeList);
        ggdm.updateMetadata(tmpFile.toPath());

        // Verify attribute was written to file
        NetcdfFileWriter ncw = NetcdfFileWriter.openExisting(tmpFile.toPath().toString());
        NetcdfFile nc = ncw.getNetcdfFile();
        assertEquals(nc.findGlobalAttribute("test_attribute").getStringValue(), "test_value");

        Files.delete(tmpFile.toPath());
    }

    @Test
    public void testCarsWeekly() throws Exception {
        String location = "src/test/resources/CARS2009_Australia_weekly.nc";
        ggdm.loadFileMetadata(new File(location));
        NcksSubsetParameters ncksSubsetParameters = ggdm.getNcksSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("DAY_OF_YEAR"));
        assertTrue(ncksSubsetParameters.containsKey("LATITUDE"));
        assertTrue(ncksSubsetParameters.containsKey("LONGITUDE"));
    }

    @Test
    public void testAcorn() throws Exception {
        String location = "src/test/resources/IMOS_ACORN_V_20090827T163000Z_CBG_FV00_1-hour-avg.nc";
        ggdm.loadFileMetadata(new File(location));
        NcksSubsetParameters ncksSubsetParameters = ggdm.getNcksSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("TIME"));
        assertTrue(ncksSubsetParameters.containsKey("LATITUDE"));
        assertTrue(ncksSubsetParameters.containsKey("LONGITUDE"));
    }

    @Test
    public void testSrs() throws Exception {
        String location = "src/test/resources/20160714152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc";
        ggdm.loadFileMetadata(new File(location));
        NcksSubsetParameters ncksSubsetParameters = ggdm.getNcksSubsetParameters();
        assertTrue(ncksSubsetParameters.containsKey("time"));
        assertTrue(ncksSubsetParameters.containsKey("lat"));
        assertTrue(ncksSubsetParameters.containsKey("lon"));
    }
}
