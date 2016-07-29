package au.org.emii.gogoduck.worker;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class URLMangler {
    private static Map<String, String> urlManglingMap = new HashMap<String, String>();

    public static void setUrlManglingMap(Map<String, String> urlManglingMap) {
        URLMangler.urlManglingMap = urlManglingMap;
    }

    public static URL mangle(URI uri) {
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

    public static URL mangle(URI uri, String property) {
        try {
            String uriStr = uri.toString();
            String key = GoGoDuckConfig.getProperties().getProperty(String.format("%s.key", property));
            String value = GoGoDuckConfig.getProperties().getProperty(String.format("%s.value", property));
            uriStr = uriStr.replaceAll(key, value);
            return new URL(uriStr);
        }
        catch (MalformedURLException e) {
            throw new GoGoDuckException(e.getMessage());
        }
    }
}
