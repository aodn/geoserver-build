package au.org.emii.gogoduck.worker;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GoGoDuckSubsetParametersTest {

    @Test
    public void testParameterParsing() {
        GoGoDuckSubsetParameters sp = new GoGoDuckSubsetParameters("TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0.0,100.0");

        assertEquals(sp.get("TIME").start, "2009-01-01T00:00:00.000Z");
        assertEquals(sp.get("TIME").end,   "2009-12-25T23:04:00.000Z");

        assertEquals(sp.get("LATITUDE").start, "-33.433849");
        assertEquals(sp.get("LATITUDE").end,   "-32.150743");

        assertEquals(sp.get("LONGITUDE").start, "114.15197");
        assertEquals(sp.get("LONGITUDE").end,   "115.741219");

        assertEquals(sp.get("DEPTH").start, "0.0");
        assertEquals(sp.get("DEPTH").end,   "100.0");
    }

}
