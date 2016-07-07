package au.org.emii.gogoduck.worker;

import java.util.ArrayList;
import java.util.List;

public class GoGoDuckModule_srs_oc extends GoGoDuckModule_srs {
    @Override
    public SubsetParameters getSubsetParameters() {
        SubsetParameters subsetParametersNew = new SubsetParameters(subset);
        subsetParametersNew.remove("TIME");

        // Rename LATITUDE -> latitude
        // Rename LONGITUDE -> longitude
        subsetParametersNew.put("latitude", subset.get("LATITUDE"));
        subsetParametersNew.put("longitude", subset.get("LONGITUDE"));
        subsetParametersNew.remove("LATITUDE");
        subsetParametersNew.remove("LONGITUDE");

        return subsetParametersNew;
    }

    @Override
    public List<String> ncksExtraParameters() {
        List<String> ncksExtraParameters = new ArrayList<String>();
        ncksExtraParameters.add("--mk_rec_dmn");
        ncksExtraParameters.add("time");
        return ncksExtraParameters;
    }
}
