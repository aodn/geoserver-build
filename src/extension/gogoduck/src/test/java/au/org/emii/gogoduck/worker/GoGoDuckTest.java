package au.org.emii.gogoduck.worker;

import org.junit.Test;
import java.net.URI;
import java.net.URL;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GoGoDuckTest {
    @Test
    public void testFileURItoURL() throws Exception {
        Method fileURItoURLMethod = GoGoDuck.class.getDeclaredMethod("fileURItoURL", URI.class);
        fileURItoURLMethod.setAccessible(true);
        URL u = (URL) fileURItoURLMethod.invoke(GoGoDuck.class, new URI("/mnt/imos-t3/file.nc"));
        assertEquals(u, new URL("https://data.aodn.org.au/file.nc"));

        u = (URL) fileURItoURLMethod.invoke(GoGoDuck.class, new URI("/mnt/opendap/2/SRS/sst/ghrsst/L3S-1d/dn/2014/20141011092000-ABOM-L3S_GHRSST-SSTfnd-AVHRR_D-1d_dn-v02.0-fv02.0.nc"));
        assertEquals(u, new URL("https://thredds.aodn.org.au/thredds/fileServer/srs/sst/ghrsst/L3S-1d/dn/2014/20141011092000-ABOM-L3S_GHRSST-SSTfnd-AVHRR_D-1d_dn-v02.0-fv02.0.nc"));
    }

    @Test
    public void testNextProfile() throws Exception {
        Method nextProfileMethod = GoGoDuck.class.getDeclaredMethod("nextProfile", String.class);
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
