package au.org.emii.gogoduck.worker;

import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class URLManglerTest {
    @Test
    public void testMangle() throws Exception {
        URLMangler.setUrlManglingMap(Main.getUrlMangling());

        URL u;
        u = URLMangler.mangle(new URI("/mnt/imos-t3/file.nc"));
        assertEquals(u, new URL("https://data.aodn.org.au/file.nc"));

        u = URLMangler.mangle(new URI("/mnt/opendap/2/SRS/sst/ghrsst/L3S-1d/dn/2014/20141011092000-ABOM-L3S_GHRSST-SSTfnd-AVHRR_D-1d_dn-v02.0-fv02.0.nc"));
        assertEquals(u, new URL("https://thredds.aodn.org.au/thredds/fileServer/srs/sst/ghrsst/L3S-1d/dn/2014/20141011092000-ABOM-L3S_GHRSST-SSTfnd-AVHRR_D-1d_dn-v02.0-fv02.0.nc"));
    }
}
