package au.org.emii.gogoduck.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("serial")
public class NcksSubsetParameters extends HashMap<String, Subset> {

    public void addTimeSubset(String netcdfVariableName, Subset timeSubset) {
        // ncks works in microseconds and time values passed to gogoduck by the portal
        // are in millisecond precision thanks to java/javascript date usage in harvesting/thredds/
        // portal code.
        // To ensure the truncated millisecond values passed select the untruncated microsecond values
        // they were derived from add the maximum possible truncation error to the end time value
        Subset ncksTimeSubset = new Subset(timeSubset.start, timeSubset.end.replaceAll("(\\.\\d\\d\\d)Z", "$1999Z"));
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
