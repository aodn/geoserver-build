package au.org.emii.gogoduck.worker;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class URLMangler {

    private static final Map<String, String> urlManglingMap = new HashMap<String, String>();

    static {
        urlManglingMap.put("/mnt/imos-t3/", "https://data.aodn.org.au/");
        urlManglingMap.put("/mnt/opendap/2/SRS/", "https://thredds.aodn.org.au/thredds/fileServer/srs/");
    }

    public static URL mangle(URI uri) {
        try {
            String uriStr = uri.toString();
            for (String key : urlManglingMap.keySet()) {
                if (uriStr.startsWith(key)) {
                    uriStr = uriStr.replace(key, urlManglingMap.get(key));
                }
            }
            return new URL(uriStr);
        }
        catch (MalformedURLException e) {
            throw new GoGoDuckException(e.getMessage());
        }
    }
}
