package au.org.emii.wps;

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
    SimpleHttpNotifier httpNotifier;
    NotifierProcess process;

    RawData notifiableData;
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
        verify(httpNotifier).notify(callbackUrl, serverUrl, "abcd-1234", callbackParams);
    }
}
