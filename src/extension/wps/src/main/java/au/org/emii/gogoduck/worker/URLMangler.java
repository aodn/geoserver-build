package au.org.emii.gogoduck.worker;

import au.org.emii.gogoduck.exception.GoGoDuckException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class URLMangler {
    private Map<String, String> urlManglingMap = new HashMap<String, String>();

    public URLMangler(Map<String, String> urlManglingMap) {
        this.urlManglingMap = urlManglingMap;
    }

    public URL mangle(URI uri) {
        try {
            String uriStr = uri.toString();
            for (String key : urlManglingMap.keySet()) {
                uriStr = uriStr.replaceAll(key, urlManglingMap.get(key));
            }
            return new URL(uriStr);
        }
        catch (MalformedURLException e) {
            throw new GoGoDuckException(e.getMessage());
        }
    }
}
