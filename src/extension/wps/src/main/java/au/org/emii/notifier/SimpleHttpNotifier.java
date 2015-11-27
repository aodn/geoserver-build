package au.org.emii.notifier;

import java.net.URL;
import org.geotools.data.ows.HTTPClient;
import org.apache.http.client.utils.URIBuilder;
import java.io.IOException;

public class SimpleHttpNotifier implements HttpNotifier {
    private final HTTPClient httpClient;

    public SimpleHttpNotifier(HTTPClient httpClient) {
        this.httpClient = httpClient;
    }

    public void notify(
        URL notificationUrl,
        URL wpsServerUrl,
        String uuid,
        boolean successful,
        String notificationParams
    ) throws IOException {
        try {
            URIBuilder builder = new URIBuilder(notificationUrl.toURI());
            builder.setParameter("server", wpsServerUrl.toExternalForm());
            builder.setParameter("uuid", uuid);
            builder.setParameter("successful", Boolean.toString(successful));

            String callbackUrlAsString = builder.build().toURL().toExternalForm();
            callbackUrlAsString += "&" + notificationParams;
            httpClient.get(new URL(callbackUrlAsString));
        }
        catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
