package au.org.emii.gogoduck.worker;

import java.net.URI;

public abstract  class GoGoDuckModule_cars extends GoGoDuckModule {
    private static final String CARS_FILENAME = "CARS2009_Australia_weekly.nc";

    @Override
    public SubsetParameters getSubsetParameters() {
        // Use TIME_OF_DAY instead of TIME
        SubsetParameters subsetParametersRenameTime = new SubsetParameters(subset);
        subsetParametersRenameTime.put("TIME_OF_DAY", subset.get("TIME"));
        subsetParametersRenameTime.remove("TIME");
        return subsetParametersRenameTime;
    }

    @Override
    public URIList getUriList() throws GoGoDuckException {
        URIList uriList = new URIList();
        uriList.add(getCarsFilename());
        return uriList;
    }

    public abstract URI getCarsFilename();
}
