package au.org.emii.wps.gogoduck.parameter;

import org.junit.Test;
import ucar.nc2.time.CalendarDate;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SubsetParametersTest {

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

    @Test
    public void testIsPointSubset() {
        SubsetParameters sp = SubsetParameters.parse("TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-33.433849;LONGITUDE,114.15197,114.15197");
        assertTrue(sp.isPointSubset());
    }

    @Test
    public void testIsNotPointSubset() {
        SubsetParameters sp = SubsetParameters.parse("TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219");
        assertFalse(sp.isPointSubset());
        assertTrue(sp.getTimeRange() != null);
    }

    @Test
    public void testAllowsMissingTimeSubset() {
        SubsetParameters sp = SubsetParameters.parse("LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219");
        assertTrue(sp.getTimeRange() == null);
    }

    @Test
    public void testCatchesInvalidTimeSubset() {
        try {
            SubsetParameters sp = SubsetParameters.parse("TIME,2009-01-01T00:00:00.000Z,totally-munted-000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid time format for subset:"));
        }
    }

    @Test
    public void testCatchesInvalidFormatTimeSubset() {
        try {
            SubsetParameters sp = SubsetParameters.parse("TIME,2014-10-10T00:00:00,2014-10-12T00:00:00;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid time format for subset:"));
        }
    }

    @Test
    public void testCatchesInvalidFormatSubset() {
        try {
            SubsetParameters sp = SubsetParameters.parse("TIME,2014-10-10T00:00:00,2014-10-12T00:00:00;LATITUDE,-33.433849,-32.150743;LONGITUDEX,114.15197,115.741219");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid latitude/longitude format for subset:"));
        }
    }
}
