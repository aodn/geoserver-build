package au.org.emii.gogoduck.worker;

public class GoGoDuckModule_cars extends GoGoDuckModule {
    @Override
    public SubsetParameters getSubsetParameters() {
        // Use TIME_OF_DAY instead of TIME
        SubsetParameters subsetParametersRenameTime = new SubsetParameters(subset);
        subsetParametersRenameTime.put("TIME_OF_DAY", subset.get("TIME"));
        subsetParametersRenameTime.remove("TIME");
        return subsetParametersRenameTime;
    }
}
