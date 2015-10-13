package au.org.emii.gogoduck.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.org.emii.gogoduck.worker.SubsetParameters.SubsetParameter;

public class SubsetParameters extends HashMap<String, SubsetParameter> {
    public class SubsetParameter {
        public String start;
        public String end;

        public SubsetParameter(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    public SubsetParameters(String subset) {
        super();
        for (String part : subset.split(";")) {
            String[] subsetParts = part.split(",");
            put(subsetParts[0], new SubsetParameter(subsetParts[1], subsetParts[2]));
        }
    }

    // Simple copy ctor
    public SubsetParameters(SubsetParameters sp) {
        super(sp);
    }

    public List<String> getNcksParameters() {
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
