package au.org.emii.gogoduck.worker;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NcksSubsetParametersTest {

    @Test
    public void testGetNcksParameters() {
        NcksSubsetParameters sp = new NcksSubsetParameters();
        sp.put("PARAM1", new Subset("1", "2"));
        sp.put("PARAM2", new Subset("2", "3"));
        sp.put("PARAM3", new Subset("4", "5"));

        assertEquals(sp.getNcksParameters().get(0), "-d");
        assertEquals(sp.getNcksParameters().get(2), "-d");
        assertEquals(sp.getNcksParameters().get(4), "-d");

        List<String> results = Arrays.asList("PARAM1,1,2", "PARAM2,2,3", "PARAM3,4,5");

        assertTrue(results.contains(sp.getNcksParameters().get(1)));
        assertTrue(results.contains(sp.getNcksParameters().get(3)));
        assertTrue(results.contains(sp.getNcksParameters().get(5)));
    }

    @Test
    public void testAddTimeSubsetAddsMaxTruncationError() {
        NcksSubsetParameters ncksSubsetParameters = new NcksSubsetParameters();
        ncksSubsetParameters.addTimeSubset("DAY_OF_YEAR", new Subset("2009-01-01T00:00:00.000Z", "2009-12-25T23:04:00.000Z"));
        assertEquals("2009-12-25T23:04:00.000999Z", ncksSubsetParameters.get("DAY_OF_YEAR").end);
    }
}
