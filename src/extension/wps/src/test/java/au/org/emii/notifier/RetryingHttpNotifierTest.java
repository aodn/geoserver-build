package au.org.emii.notifier;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.mockito.Mockito.*;

public class RetryingHttpNotifierTest {

    HttpNotifier wrappedNotifier;
    RetryingHttpNotifier retryingNotifier;
    int notificationAttempts = 3;
    int retryInterval = 0;

    URL notificationUrl;
    URL wpsServerUrl;
    String uuid = "uuid";
    String notificationParams = "notificationParams";
    boolean successful = true;

    @Before
    public void setUp() throws IOException {
        notificationUrl = new URL("http://notificationurl.com");
        wpsServerUrl = new URL("http://wpsServer.com");

        wrappedNotifier = mock(SimpleHttpNotifier.class);
        retryingNotifier = new RetryingHttpNotifier(wrappedNotifier, notificationAttempts, retryInterval);
    }

    @Test(expected = IOException.class)
    public void testExecuteRetriesFixedNumberOfTimes() throws IOException {
        doThrow(new IOException()).when(wrappedNotifier).notify(notificationUrl, wpsServerUrl, uuid, successful, notificationParams);

        retryingNotifier.notify(notificationUrl, wpsServerUrl, uuid, successful, notificationParams);

        verify(wrappedNotifier, times(notificationAttempts)).notify(notificationUrl, wpsServerUrl, uuid, successful, notificationParams);
    }

    @Test
    public void testExecuteRetriesUntilSuccess() throws IOException {
        doThrow(new IOException()).doNothing(/* will succeed */).when(wrappedNotifier).notify(notificationUrl, wpsServerUrl, uuid, successful, notificationParams);

        retryingNotifier.notify(notificationUrl, wpsServerUrl, uuid, successful, notificationParams);

        verify(wrappedNotifier, times(2)).notify(notificationUrl, wpsServerUrl, uuid, successful, notificationParams);
    }
}
