package au.org.emii.wps.gogoduck.index;

import au.org.emii.wps.gogoduck.index.URLMangler;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class URLManglerTest {
    @Test
    public void testMangle() throws Exception {
        Map<String, String> urlMangling = new HashMap<String, String>();
        urlMangling.put("^/mnt/imos-t3/", "https://data.aodn.org.au/");
        urlMangling.put("^/mnt/opendap/2/SRS/", "https://thredds.aodn.org.au/thredds/fileServer/IMOS/SRS/");
        urlMangling.put("^IMOS/", "http://imos-data.aodn.org.au/IMOS/");

        URLMangler urlMangler = new URLMangler(urlMangling);

        URL u;
        u = urlMangler.mangle(new URI("/mnt/imos-t3/file.nc"));
        assertEquals(u, new URL("https://data.aodn.org.au/file.nc"));

        u = urlMangler.mangle(new URI("/mnt/opendap/2/SRS/sst/ghrsst/L3S-1d/dn/2014/20141011092000-ABOM-L3S_GHRSST-SSTfnd-AVHRR_D-1d_dn-v02.0-fv02.0.nc"));
        assertEquals(u, new URL("https://thredds.aodn.org.au/thredds/fileServer/IMOS/SRS/sst/ghrsst/L3S-1d/dn/2014/20141011092000-ABOM-L3S_GHRSST-SSTfnd-AVHRR_D-1d_dn-v02.0-fv02.0.nc"));
    }
}
