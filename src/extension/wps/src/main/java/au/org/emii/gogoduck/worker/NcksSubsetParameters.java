package au.org.emii.gogoduck.worker;

import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("serial")
public class NcksSubsetParameters extends HashMap<String, Subset> {

    public void addTimeSubset(String netcdfVariableName, Subset timeSubset) {
        // ncks works in microseconds and time values passed to gogoduck by the portal
        // are in millisecond precision thanks to java/javascript date usage in harvesting/thredds/
        // portal code.
        // To ensure the rounded millisecond values passed select the unrounded microsecond values
        // they were derived from add the possible millisecond rounding or truncation error to the from and to times.
        CalendarDate start = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, timeSubset.start);
        start = start.add(-1, CalendarPeriod.Field.Millisec);
        CalendarDate end = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, timeSubset.end);
        end = end.add(1, CalendarPeriod.Field.Millisec);
        Subset ncksTimeSubset = new Subset(CalendarDateFormatter.toDateTimeStringISO(start), CalendarDateFormatter.toDateTimeStringISO(end));
        put(netcdfVariableName, ncksTimeSubset);
    }

    public synchronized List<String> getNcksParameters() {
        List<String> ncksParameters = new ArrayList<String>();

        for (String key : keySet()) {
            ncksParameters.add("-d");
            ncksParameters.add(String.format("%s,%s,%s", key, get(key).start, get(key).end));
        }

        return ncksParameters;
    }

    public String toString() {
        return getNcksParameters().toString();
    }

}
