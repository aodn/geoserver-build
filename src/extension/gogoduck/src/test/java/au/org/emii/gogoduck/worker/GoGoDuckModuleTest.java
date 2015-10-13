package au.org.emii.gogoduck.worker;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import au.org.emii.gogoduck.worker.GoGoDuck.UserLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

public class GoGoDuckModuleTest {
    public GoGoDuckModule ggdm = null;

    @Before
    public void beforeEach() {
        ggdm = new GoGoDuckModule();
        ggdm.init("", "", "TIME,1,2;LONGITUDE,2,3", null);
    }

    @Test
    public void testGetSubsetParameters() throws Exception {
        // Make sure it removes `TIME` subset parameter
        assertFalse(ggdm.getSubsetParameters().containsKey("TIME"));

        // But did that only on a copy of the subset parameters
        Field privateSubset = GoGoDuckModule.class.getDeclaredField("subset");
        privateSubset.setAccessible(true);
        SubsetParameters sp = (SubsetParameters) privateSubset.get(ggdm);
        assertTrue(sp.containsKey("TIME"));
    }

    public class GoGoDuckModule_test extends GoGoDuckModule {
        @Override
        protected List<Attribute> getGlobalAttributesToUpdate(NetcdfFile nc) {
            List<Attribute> newAttributeList = new ArrayList<Attribute>();
            newAttributeList.add(new Attribute("test_attribute", "test_value"));
            return newAttributeList;
        }
    }

    @Test
    public void testUpdateMetadata() throws Exception {
        File sampleNetcdf = new File("src/test/resources/file.nc").getAbsoluteFile();
        File tmpFile = File.createTempFile("tmp", ".nc");
        Files.delete(tmpFile.toPath());
        Files.copy(sampleNetcdf.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ggdm = new GoGoDuckModule_test();
        ggdm.init("", "", "TIME,1,2;LONGITUDE,2,3", null);
        ggdm.updateMetadata(tmpFile.toPath());

        // Verify attribute was written to file
        NetcdfFile nc = NetcdfFileWriter.openExisting(tmpFile.toPath().toString()).getNetcdfFile();
        assertEquals(nc.findGlobalAttribute("test_attribute").getStringValue(), "test_value");

        Files.delete(tmpFile.toPath());
    }
}
