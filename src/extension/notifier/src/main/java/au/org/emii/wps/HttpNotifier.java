package au.org.emii.wps;

import java.net.URL;
import org.geotools.data.ows.HTTPClient;
import org.apache.http.client.utils.URIBuilder;
import java.io.IOException;

public class HttpNotifier {
    private final HTTPClient httpClient;

    public HttpNotifier(HTTPClient httpClient) {
        this.httpClient = httpClient;
    }

    public void notify(
        URL notificationUrl,
        URL wpsServerUrl,
        String uuid,
        String notificationParams
    ) throws IOException {
        try {
            URIBuilder builder = new URIBuilder(notificationUrl.toURI());
            builder.setParameter("server", wpsServerUrl.toExternalForm());
            builder.setParameter("uuid", uuid);

            String callbackUrlAsString = builder.build().toURL().toExternalForm();
            callbackUrlAsString += "&" + notificationParams;
            httpClient.get(new URL(callbackUrlAsString));
        }
        catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
