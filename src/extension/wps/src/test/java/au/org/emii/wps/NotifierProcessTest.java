package au.org.emii.wps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import org.geoserver.wps.process.RawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.junit.Before;
import org.junit.Test;

import au.org.emii.notifier.SimpleHttpNotifier;

public class NotifierProcessTest {

    WPSResourceManager resourceManager;
    SimpleHttpNotifier httpNotifier;
    NotifierProcess process;

    RawData wrappedProcessResponse;
    URL callbackUrl;
    String callbackParams;

    URL serverUrl;

    @Before
    public void setUp() throws IOException {
        resourceManager = mock(WPSResourceManager.class);
        when(resourceManager.getExecutionId(true)).thenReturn("abcd-1234");

        httpNotifier = mock(SimpleHttpNotifier.class);

        process = spy(new NotifierProcess(resourceManager, httpNotifier));
        serverUrl = new URL("http://wpsserver.com");
        doReturn(serverUrl).when(process).getWpsUrl();

        wrappedProcessResponse = mock(RawData.class);
        callbackUrl = new URL("http://example.com");
        callbackParams = "email.to=bob@example.com";
    }

    @Test
    public void testExecuteReturnsGivenData() throws IOException {
        assertEquals(wrappedProcessResponse, process.execute(wrappedProcessResponse, callbackUrl, callbackParams));
    }

    @Test
    public void testExecuteNotifiesViaCallback() throws IOException {
        process.execute(wrappedProcessResponse, callbackUrl, callbackParams);
        verify(httpNotifier).notify(callbackUrl, serverUrl, "abcd-1234", callbackParams);
    }
}
