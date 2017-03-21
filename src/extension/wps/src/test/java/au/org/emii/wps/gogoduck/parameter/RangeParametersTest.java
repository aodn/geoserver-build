package au.org.emii.wps.gogoduck.parameter;

import org.junit.Test;
import ucar.nc2.time.CalendarDate;

import static org.junit.Assert.assertEquals;

public class RangeParametersTest {

    @Test
    public void testParameterParsing() {
        SubsetParameters sp = SubsetParameters.parse("TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219");

        assertEquals(sp.getTimeRange().getStart(), CalendarDate.parseISOformat("gregorian", "2009-01-01T00:00:00.000Z"));
        assertEquals(sp.getTimeRange().getEnd(), CalendarDate.parseISOformat("gregorian","2009-12-25T23:04:00.000Z"));

        assertEquals(sp.getBbox().getLatMin(), -33.433849, 0.000001);
        assertEquals(sp.getBbox().getLatMax(), -32.150743, 0.000001);

        assertEquals(sp.getBbox().getLonMin(), 114.15197, 0.000001);
        assertEquals(sp.getBbox().getLonMax(), 115.741219, 0.000001);
    }

}
