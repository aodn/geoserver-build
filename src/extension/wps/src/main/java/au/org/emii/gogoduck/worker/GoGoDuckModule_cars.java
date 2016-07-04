package au.org.emii.gogoduck.worker;

import java.net.URI;

public abstract class GoGoDuckModule_cars extends GoGoDuckModule {
    @Override
    public NcksSubsetParameters getNcksSubsetParameters() {
        NcksSubsetParameters ncksSubsetParameters = new NcksSubsetParameters();
        ncksSubsetParameters.put("LATITUDE", subset.get("LATITUDE"));
        ncksSubsetParameters.put("LONGITUDE", subset.get("LONGITUDE"));
        ncksSubsetParameters.addTimeSubset("DAY_OF_YEAR", subset.get("TIME"));
        return ncksSubsetParameters;
    }

    @Override
    public URIList getUriList() throws GoGoDuckException {
        URIList uriList = new URIList();
        uriList.add(getCarsFilename());
        return uriList;
    }

    public abstract URI getCarsFilename();
}
