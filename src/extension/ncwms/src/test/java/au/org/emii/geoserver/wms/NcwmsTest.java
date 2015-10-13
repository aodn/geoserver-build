package au.org.emii.geoserver.wms;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import au.org.emii.geoserver.wms.Ncwms;
import org.junit.Test;

public class NcwmsTest {

    @Test
    public void testGetUniqueDates() throws Exception {
        String csvString = "FID,time\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c99,2010-02-23T06:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c98,2010-02-23T08:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c97,2010-03-09T08:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c96,2010-03-10T09:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c95,2010-03-10T10:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c94,2010-03-11T15:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c93,2011-01-22T18:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c92,2011-01-22T20:30:00\n";
        ByteArrayInputStream csv = new ByteArrayInputStream(csvString.getBytes(StandardCharsets.UTF_8));

        Map<Integer, Map<Integer, Set<Integer>> > expected =
            new HashMap<Integer , Map<Integer, Set<Integer> > >();

        expected.put(2010, new HashMap<Integer, Set<Integer> >());
        expected.get(2010).put(1, new HashSet<Integer>());
        expected.get(2010).get(1).add(23);
        expected.get(2010).put(2, new HashSet<Integer>());
        expected.get(2010).get(2).add(9);
        expected.get(2010).get(2).add(10);
        expected.get(2010).get(2).add(11);

        expected.put(2011, new HashMap<Integer, Set<Integer> >());
        expected.get(2011).put(0, new HashSet<Integer>());
        expected.get(2011).get(0).add(22);

        assertEquals(expected.toString(), Ncwms.getUniqueDates(csv).toString());
    }

    @Test
    public void testGetWmsUrl() throws Exception {
        String csvString = "FID,time\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502c191468_-7d5a,/mnt/imos-t3/IMOS/opendap/ACORN/gridded_1h-avg-current-map_QC/ROT/2014/06/06/IMOS_ACORN_V_20140606T003000Z_ROT_FV01_1-hour-avg.nc\n";
        ByteArrayInputStream csv = new ByteArrayInputStream(csvString.getBytes(StandardCharsets.UTF_8));

        Map<String, String> urlSubstitutions = new HashMap<String, String>();
        urlSubstitutions.put("/mnt/imos-t3/IMOS/opendap/", "REPLACED/");

        String expected = "REPLACED/ACORN/gridded_1h-avg-current-map_QC/ROT/2014/06/06/IMOS_ACORN_V_20140606T003000Z_ROT_FV01_1-hour-avg.nc";
        assertEquals(expected, Ncwms.getWmsUrl(csv, urlSubstitutions));
    }

    @Test
    public void testGetTimesForDay() throws Exception {
        String csvString = "FID,time\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c97,2010-03-10T08:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c96,2010-03-10T09:30:00\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502bdb829d_-1c95,2010-03-10T10:30:00\n";
        ByteArrayInputStream csv = new ByteArrayInputStream(csvString.getBytes(StandardCharsets.UTF_8));

        List<String> expected = new ArrayList<String>() {{
            add("08:30:00.000Z");
            add("09:30:00.000Z");
            add("10:30:00.000Z");
        }};

        assertEquals(expected.toString(), Ncwms.getTimesForDay(csv).toString());
    }
}
