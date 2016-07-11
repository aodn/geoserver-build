package au.org.emii.gogoduck.worker;

import java.util.ArrayList;
import java.util.List;

public class GoGoDuckModule_srs_oc extends GoGoDuckModule_srs {
    @Override
    public NcksSubsetParameters getNcksSubsetParameters() {
        NcksSubsetParameters ncksSubsetParameters = new NcksSubsetParameters();
        ncksSubsetParameters.put("latitude", subset.get("LATITUDE"));
        ncksSubsetParameters.put("longitude", subset.get("LONGITUDE"));
        return ncksSubsetParameters;
    }

    @Override
    public List<String> ncksExtraParameters() {
        List<String> ncksExtraParameters = new ArrayList<String>();
        ncksExtraParameters.add("--mk_rec_dmn");
        ncksExtraParameters.add("time");
        return ncksExtraParameters;
    }
}
