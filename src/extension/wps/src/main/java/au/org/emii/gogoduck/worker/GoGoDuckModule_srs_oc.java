package au.org.emii.gogoduck.worker;

public class GoGoDuckModule_srs_oc extends GoGoDuckModule_srs {
    @Override
    public SubsetParameters getSubsetParameters() {
        SubsetParameters subsetParametersNew = new SubsetParameters(subset);
        subsetParametersNew.remove("TIME");

        // Rename LATITUDE -> latitude
        // Rename LONGITUDE -> longitude
        subsetParametersNew.put("latitude", subset.get("LATITUDE"));
        subsetParametersNew.put("longitude", subset.get("LONGITUDE"));

        return subsetParametersNew;
    }
}
