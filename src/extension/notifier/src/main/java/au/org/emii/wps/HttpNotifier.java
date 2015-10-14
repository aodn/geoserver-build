package au.org.emii.wps;

import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.geotools.data.ows.HTTPClient;
import org.apache.http.client.utils.URIBuilder;
import java.io.IOException;
import java.net.URISyntaxException;

public class HttpNotifier {
    private static final Logger logger = LoggerFactory.getLogger(NotifierProcess.class);

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
