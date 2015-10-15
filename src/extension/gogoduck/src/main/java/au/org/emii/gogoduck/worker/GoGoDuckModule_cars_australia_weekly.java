package au.org.emii.gogoduck.worker;

import java.net.URI;
import java.net.URISyntaxException;

public class GoGoDuckModule_cars_australia_weekly extends GoGoDuckModule_cars {
    private static final String CARS_FILENAME = "CARS2009_Australia_weekly.nc";

    @Override
    public URI getCarsFilename() {
        try {
            return new URI(CARS_FILENAME);
        }
        catch (URISyntaxException e) {
            throw new GoGoDuckException("Could not add URI for cars_australia_weekly");
        }
    }
}
