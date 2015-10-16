package au.org.emii.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ URL.class })
public class NcwmsTest {
    private Ncwms ncwms = null;

    @Before
    public void setUp() throws Exception {
        ncwms = new Ncwms();
    }

    private Ncwms mockWfsQuery(String query, String response) throws Exception {
        Ncwms ncwmsPartialMock = EasyMock
                .createMockBuilder(Ncwms.class)
                .addMockedMethod("wfsQuery")
                .createMock();

        EasyMock.expect(ncwmsPartialMock.wfsQuery(query))
                .andReturn(IOUtils.toInputStream(response, "UTF-8"));

        return ncwmsPartialMock;
    }

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

        assertEquals(expected.toString(), ncwms.getUniqueDates(csv).toString());
    }

    @Test
    public void testGetWmsUrlTimeUnspecified() throws Exception {
        String csvString = "FID,time\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502c191468_-7d5a,/mnt/imos-t3/IMOS/opendap/ACORN/gridded_1h-avg-current-map_QC/ROT/2014/06/06/IMOS_ACORN_V_20140606T003000Z_ROT_FV01_1-hour-avg.nc\n";
        ByteArrayInputStream csv = new ByteArrayInputStream(csvString.getBytes(StandardCharsets.UTF_8));

        Map<String, String> urlSubstitutions = new HashMap<String, String>();
        urlSubstitutions.put("/mnt/imos-t3/IMOS/opendap/", "http://some_url/");
        Ncwms.urlSubstitutions = urlSubstitutions;

        String expected = "http://some_url/ACORN/gridded_1h-avg-current-map_QC/ROT/2014/06/06/IMOS_ACORN_V_20140606T003000Z_ROT_FV01_1-hour-avg.nc";

        String expectedQuery =
                "typeName=acorn_hourly_avg_rot_qc_timeseries_url&SERVICE=WFS&outputFormat=csv" +
                "&REQUEST=GetFeature&VERSION=1.0.0&PROPERTYNAME=time" +
                "&maxFeatures=1&sortBy=time+D";

        Ncwms ncwmsMockWfsQuery = mockWfsQuery(expectedQuery, csvString);
        EasyMock.replay(ncwmsMockWfsQuery);
        assertEquals(expected, ncwmsMockWfsQuery.getWmsUrl("acorn_hourly_avg_rot_qc_timeseries_url", null));
        EasyMock.verify(ncwmsMockWfsQuery);
    }

    @Test
    public void testGetWmsUrlTimeSpecified() throws Exception {
        String csvString = "FID,time\n";
        csvString += "acorn_hourly_avg_rot_qc_timeseries_url.fid-607eb861_1502c191468_-7d5a,/mnt/imos-t3/IMOS/opendap/ACORN/gridded_1h-avg-current-map_QC/ROT/2014/06/06/IMOS_ACORN_V_20140606T003000Z_ROT_FV01_1-hour-avg.nc\n";
        ByteArrayInputStream csv = new ByteArrayInputStream(csvString.getBytes(StandardCharsets.UTF_8));

        String time = "2014-06-06T00:30:00Z";

        Map<String, String> urlSubstitutions = new HashMap<String, String>();
        urlSubstitutions.put("/mnt/imos-t3/IMOS/opendap/", "http://some_url/");
        Ncwms.urlSubstitutions = urlSubstitutions;

        String expected = "http://some_url/ACORN/gridded_1h-avg-current-map_QC/ROT/2014/06/06/IMOS_ACORN_V_20140606T003000Z_ROT_FV01_1-hour-avg.nc";

        String expectedQuery =
                "typeName=acorn_hourly_avg_rot_qc_timeseries_url&SERVICE=WFS&outputFormat=csv" +
                "&REQUEST=GetFeature&VERSION=1.0.0&PROPERTYNAME=time" +
                "&CQL_FILTER=time+%3D+" + URLEncoder.encode(time, StandardCharsets.UTF_8.name()) + "&maxFeatures=1";

        Ncwms ncwmsMockWfsQuery = mockWfsQuery(expectedQuery, csvString);
        EasyMock.replay(ncwmsMockWfsQuery);
        assertEquals(expected, ncwmsMockWfsQuery.getWmsUrl("acorn_hourly_avg_rot_qc_timeseries_url", "2014-06-06T00:30:00Z"));
        EasyMock.verify(ncwmsMockWfsQuery);
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

        assertEquals(expected.toString(), ncwms.getTimesForDay(csv).toString());
    }
}
