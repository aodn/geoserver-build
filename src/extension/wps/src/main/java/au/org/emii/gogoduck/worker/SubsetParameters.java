package au.org.emii.gogoduck.worker;

import java.util.*;

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

    private static final Set<String> floatVariableNames = new HashSet<String>() {{
        add("depth");
        add("latitude");
        add("longitude");
        add("lat");
        add("lon");
    }};

    public SubsetParameters(String subset) {
        super();
        for (String part : subset.split(";")) {
            String[] subsetParts = part.split(",");
            String key = subsetParts[0];
            String start = fixFloat(key, subsetParts[1]);
            String end = fixFloat(key, subsetParts[2]);
            put(key, new SubsetParameter(start, end));
        }
    }

    // Simple copy ctor
    public SubsetParameters(SubsetParameters sp) {
        super(sp);
    }

    public synchronized List<String> getNcksParameters() {
        List<String> ncksParameters = new ArrayList<String>();

        for (String key : keySet()) {
            ncksParameters.add("-d");
            ncksParameters.add(String.format("%s,%s,%s", key, get(key).start, get(key).end));
        }

        return ncksParameters;
    }

    private String fixFloat(String key, String value) {
        if (floatVariableNames.contains(key.toLowerCase()) && ! value.contains(".")) {
            return String.format("%s.0", value);
        }
        else {
            return value;
        }
    }


    public String toString() {
        return getNcksParameters().toString();
    }
}
