package au.org.emii.gogoduck.worker;

import java.net.URI;
import java.net.URISyntaxException;

public class GoGoDuckModule_cars_world_monthly extends GoGoDuckModule_cars {
    @Override
    public URIList getUriList() throws GoGoDuckException {
        URIList uriList = new URIList();
        try {
            uriList.add(new URI("/mnt/imos-t3/climatology/CARS/2009/eMII-product/CARS2009_World_monthly.nc"));
        }
        catch (URISyntaxException e) {
            throw new GoGoDuckException("Could not add URI for cars_world_monthly");
        }
        return uriList;
    }
}
