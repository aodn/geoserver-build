package au.org.emii.gogoduck.worker;

import static org.junit.Assert.assertEquals;

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

public class GoGoDuckModuleTest {
    public GoGoDuckModule ggdm = null;

    @Before
    public void beforeEach() {
        ggdm = new GoGoDuckModule();
        ggdm.init("", new HttpIndexReader(null, ""), "TIME,1,2;LONGITUDE,2,3", null);
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
        ggdm.init("", new HttpIndexReader(null, ""), "TIME,1,2;LONGITUDE,2,3", null);
        ggdm.updateMetadata(tmpFile.toPath());

        // Verify attribute was written to file
        NetcdfFile nc = NetcdfFileWriter.openExisting(tmpFile.toPath().toString()).getNetcdfFile();
        assertEquals(nc.findGlobalAttribute("test_attribute").getStringValue(), "test_value");

        Files.delete(tmpFile.toPath());
    }


    @Test
    public void testNextProfile() throws Exception {
        Method nextProfileMethod = GoGoDuckModule.class.getDeclaredMethod("nextProfile", String.class);
        nextProfileMethod.setAccessible(true);

        String profile = "this_is_a_profile";

        profile = (String) nextProfileMethod.invoke(GoGoDuck.class, profile);
        assertEquals(profile, "this_is_a");

        profile = (String) nextProfileMethod.invoke(GoGoDuck.class, profile);
        assertEquals(profile, "this_is");

        profile = (String) nextProfileMethod.invoke(GoGoDuck.class, profile);
        assertEquals(profile, "this");
    }
}
