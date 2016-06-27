package au.org.emii.gogoduck.worker;

import java.net.URI;
import java.net.URISyntaxException;

public class GoGoDuckModule_cars_world_monthly extends GoGoDuckModule_cars {
    private static final String CARS_FILENAME = "CSIRO/Climatology/CARS/2009/eMII-product/CARS2009_World_monthly.nc";

    @Override
    public URI getCarsFilename() {
        try {
            return new URI(CARS_FILENAME);
        }
        catch (URISyntaxException e) {
            throw new GoGoDuckException("Could not add URI for cars_australia_monthly");
        }
    }
}
