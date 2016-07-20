package au.org.emii.gogoduck.worker;

import java.util.HashMap;

@SuppressWarnings("serial")
public class GoGoDuckSubsetParameters extends HashMap<String, Subset> {

    public GoGoDuckSubsetParameters(String subset) {
        super();
        for (String part : subset.split(";")) {
            String[] subsetParts = part.split(",");
            put(subsetParts[0], new Subset(subsetParts[1], subsetParts[2]));
        }
    }

}
