package au.org.emii.gogoduck.worker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubsetParametersTest {

    @Test
    public void testParameterParsing() {
        SubsetParameters sp = new SubsetParameters("TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0.0,100.0");

        assertEquals(sp.get("TIME").start, "2009-01-01T00:00:00.000Z");
        assertEquals(sp.get("TIME").end,   "2009-12-25T23:04:00.000Z");

        assertEquals(sp.get("LATITUDE").start, "-33.433849");
        assertEquals(sp.get("LATITUDE").end,   "-32.150743");

        assertEquals(sp.get("LONGITUDE").start, "114.15197");
        assertEquals(sp.get("LONGITUDE").end,   "115.741219");

        assertEquals(sp.get("DEPTH").start, "0.0");
        assertEquals(sp.get("DEPTH").end,   "100.0");
    }

    @Test
    public void testGetNcksParameters() {
        SubsetParameters sp = new SubsetParameters("PARAM1,1,2;PARAM2,2,3;PARAM3,4,5");

        assertEquals(sp.getNcksParameters().get(0), "-d");
        assertEquals(sp.getNcksParameters().get(1), "PARAM3,4,5");

        assertEquals(sp.getNcksParameters().get(2), "-d");
        assertEquals(sp.getNcksParameters().get(3), "PARAM1,1,2");

        assertEquals(sp.getNcksParameters().get(4), "-d");
        assertEquals(sp.getNcksParameters().get(5), "PARAM2,2,3");
    }
}
