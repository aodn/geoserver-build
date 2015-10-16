package au.org.emii.wps;

import org.geotools.process.ProcessException;
import org.junit.Before;
import org.junit.Test;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geoserver.wps.process.RawData;

import java.net.URL;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NotifierProcessTest {

    WPSResourceManager resourceManager;
    HttpNotifier httpNotifier;
    int retryAttempts = 3;
    int retryInterval = 0;
    NotifierProcess process;

    RawData notifiableData;
    URL callbackUrl;
    String callbackParams;
    String executionId = "abcd-1234";

    URL serverUrl;

    @Before
    public void setUp() throws IOException {
        resourceManager = mock(WPSResourceManager.class);
        when(resourceManager.getExecutionId(true)).thenReturn(executionId);

        httpNotifier = mock(HttpNotifier.class);

        process = spy(new NotifierProcess(resourceManager, httpNotifier, retryAttempts, retryInterval));
        serverUrl = new URL("http://wpsserver.com");
        doReturn(serverUrl).when(process).getWpsUrl();

        notifiableData = mock(RawData.class);
        callbackUrl = new URL("http://example.com");
        callbackParams = "email.to=bob@example.com";
    }

    @Test
    public void testExecuteReturnsGivenData() throws IOException {
        assertEquals(notifiableData, process.execute(notifiableData, callbackUrl, callbackParams));
    }

    @Test
    public void testExecuteNotifiesViaCallback() throws IOException {
        process.execute(notifiableData, callbackUrl, callbackParams);
        verify(httpNotifier).notify(callbackUrl, serverUrl, executionId, callbackParams);
    }

    @Test(expected = ProcessException.class)
    public void testExecuteRetriesFixedNumberOfTimes() throws IOException {
        doThrow(new IOException()).when(httpNotifier).notify(callbackUrl, serverUrl, executionId, callbackParams);

        process.execute(notifiableData, callbackUrl, callbackParams);

        verify(httpNotifier, times(retryAttempts)).notify(callbackUrl, serverUrl, executionId, callbackParams);
    }

    @Test
    public void testExecuteRetriesUntilSuccess() throws IOException {
        doThrow(new IOException()).doNothing(/* will succeed */).when(httpNotifier).notify(callbackUrl, serverUrl, executionId, callbackParams);

        process.execute(notifiableData, callbackUrl, callbackParams);

        verify(httpNotifier, times(2)).notify(callbackUrl, serverUrl, executionId, callbackParams);
    }
}
