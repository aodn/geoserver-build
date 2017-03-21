package au.org.emii.wps.gogoduck.parameter;

import au.org.emii.wps.gogoduck.exception.GoGoDuckException;
import ucar.ma2.Range;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubsetParameters {
    private final LatLonRect bbox;
    private final CalendarDateRange timeRange;

    public SubsetParameters(LatLonRect bbox, CalendarDateRange timeRange) {
        this.bbox = bbox;
        this.timeRange = timeRange;
    }

    public LatLonRect getBbox() {
        return bbox;
    }

    public CalendarDateRange getTimeRange() {
        return timeRange;
    }

    public Range getVerticalRange() {
        return null;
    }

    public static SubsetParameters parse(String subset) {
        //Validation

        int timeCount = 0, latLonCount = 0;
        Pattern timePattern = Pattern.compile("((1[7-9]|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])T([0-9]+):([0-5]?[0-9]):([0-5]?[0-9])");
        Pattern latLonPattern = Pattern.compile("([+-]?\\d+\\.?\\d+)\\s*,\\s*([+-]?\\d+\\.?\\d+)");

        Matcher matcher = timePattern.matcher(subset);

        while (matcher.find()) {
            timeCount++;
        }

        if (timeCount != 2) {
            throw new GoGoDuckException(String.format("Invalid time format for subset: %s", subset));
        }

        matcher = latLonPattern.matcher(subset);
        while (matcher.find()) {
            latLonCount++;
        }

        if (latLonCount != 2) {
            throw new GoGoDuckException(String.format("Invalid latitude/longitude format for subset: %s", subset));
        }

        // Parse
        //TODO: validate here!
        Map<String, ParameterRange> subsets = new HashMap<>();

        for (String part : subset.split(";")) {
            String[] subsetParts = part.split(",");
            subsets.put(subsetParts[0], new ParameterRange(subsetParts[1], subsetParts[2]));
        }

        ParameterRange latitudeRange = subsets.get("LATITUDE");
        ParameterRange longitudeRange = subsets.get("LONGITUDE");
        ParameterRange timeRange = subsets.get("TIME");

        Double latMin = Double.parseDouble(latitudeRange.start);
        Double latMax = Double.parseDouble(latitudeRange.end);
        Double lonMin = Double.parseDouble(longitudeRange.start);
        Double lonMax = Double.parseDouble(longitudeRange.end);

        LatLonRect bbox = new LatLonRect(new LatLonPointImpl(latMin, lonMin), new LatLonPointImpl(latMax, lonMax));

        CalendarDate startTime = CalendarDate.parseISOformat("gregorian", timeRange.start);
        CalendarDate endTime = CalendarDate.parseISOformat("gregorian", timeRange.end);
        CalendarDateRange calendarDateRange = CalendarDateRange.of(startTime, endTime);

        return new SubsetParameters(bbox, calendarDateRange);
    }

    private static class ParameterRange {
        String start;
        String end;

        ParameterRange(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

}
