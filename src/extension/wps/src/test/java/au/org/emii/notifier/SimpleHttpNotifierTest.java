package au.org.emii.notifier;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import org.geotools.data.ows.SimpleHttpClient;
import org.geotools.data.ows.HTTPClient;

import static org.mockito.Mockito.*;

public class SimpleHttpNotifierTest {

    @Test
    public void testNotifyMakeHttpRequest() throws IOException {
        HTTPClient httpClient = mock(SimpleHttpClient.class);
        SimpleHttpNotifier notifier = new SimpleHttpNotifier(httpClient);

        URL notificationUrl = new URL("http://notifiee");
        URL wpsServiceUrl = new URL("http://notifier");
        String id = "1234";
        boolean successful = true;

        String notificationParams = "foo=bar";

        notifier.notify(notificationUrl, wpsServiceUrl, id, successful, notificationParams);

        URL expectedUrl = new URL("http://notifiee?server=http%3A%2F%2Fnotifier&uuid=1234&successful=true&foo=bar");
        verify(httpClient).get(expectedUrl);
    }
}
