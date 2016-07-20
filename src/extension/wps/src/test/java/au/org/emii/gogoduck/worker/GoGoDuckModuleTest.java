package au.org.emii.gogoduck.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;

public class GoGoDuckModuleTest {
    public GoGoDuckModule ggdm = null;

    @Before
    public void beforeEach() {
        ggdm = new GoGoDuckModule("", new HttpIndexReader(null, ""), "TIME,1,2;LONGITUDE,2,3", null);
    }

    @Test
    public void testUpdateMetadata() throws Exception {
        File sampleNetcdf = new File("src/test/resources/file.nc").getAbsoluteFile();
        File tmpFile = File.createTempFile("tmp", ".nc");
        Files.delete(tmpFile.toPath());
        Files.copy(sampleNetcdf.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ggdm = new GoGoDuckModule();
        ggdm.init("", new HttpIndexReader(null, ""), "TIME,1,2;LONGITUDE,2,3", null);
        ggdm.updateMetadata(tmpFile.toPath());

        // Writing to file
        NetcdfFileWriter ncw = NetcdfFileWriter.openExisting(tmpFile.toPath().toString());
        NetcdfFile nc = ncw.getNetcdfFile();
        ncw.setRedefineMode(true);
        ncw.addGroupAttribute(null, new Attribute("test_attribute", "test_value"));
        ncw.setRedefineMode(false);
        ncw.close();

        // Verify attribute was written to file
        assertEquals(nc.findGlobalAttribute("test_attribute").getStringValue(), "test_value");

        Files.delete(tmpFile.toPath());
    }

    @Test
    public void testCarsWeekly() throws Exception {
        String location = "src/test/resources/CARS2009_Australia_weekly.nc";
        NcksSubsetParameters ncksSubsetParameters = ggdm.getNcksSubsetParameters(location);
        assertTrue(ncksSubsetParameters.containsKey("DAY_OF_YEAR"));
        assertTrue(ncksSubsetParameters.containsKey("LATITUDE"));
        assertTrue(ncksSubsetParameters.containsKey("LONGITUDE"));
    }

    @Test
    public void testAcorn() throws Exception {
        String location = "src/test/resources/IMOS_ACORN_V_20090827T163000Z_CBG_FV00_1-hour-avg.nc";
        NcksSubsetParameters ncksSubsetParameters = ggdm.getNcksSubsetParameters(location);
        assertTrue(ncksSubsetParameters.containsKey("TIME"));
        assertTrue(ncksSubsetParameters.containsKey("LATITUDE"));
        assertTrue(ncksSubsetParameters.containsKey("LONGITUDE"));
    }

    @Test
    public void testSrs() throws Exception {
        String location = "src/test/resources/20160714152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc";
        NcksSubsetParameters ncksSubsetParameters = ggdm.getNcksSubsetParameters(location);
        assertTrue(ncksSubsetParameters.containsKey("time"));
        assertTrue(ncksSubsetParameters.containsKey("lat"));
        assertTrue(ncksSubsetParameters.containsKey("lon"));
    }
}
