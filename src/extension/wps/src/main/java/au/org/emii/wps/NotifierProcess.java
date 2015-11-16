package au.org.emii.wps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.opengis.wps10.ExecuteType;

import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.Operation;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.StringRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.emii.notifier.HttpNotifier;

@DescribeProcess(title="Notifier", description="Notify subscribers when a WPS process completes")
public class NotifierProcess implements GeoServerProcess {
    private static final Logger logger = LoggerFactory.getLogger(NotifierProcess.class);

    private final WPSResourceManager resourceManager;
    private final HttpNotifier httpNotifier;

    public NotifierProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier) {
        this.resourceManager = resourceManager;
        this.httpNotifier = httpNotifier;
    }

    @DescribeResult(name="result", description="Wrapped process response", meta={"mimeTypes=application/xml"})
    public StringRawData execute(
        @DescribeParameter(name="wrappedProcessResponse", description="Wrapped process response")
        StringRawData response,
        @DescribeParameter(name="callbackUrl", description="Callback URL")
        URL callbackUrl,
        @DescribeParameter(name="callbackParams", description="Parameters to append to the callback")
        String callbackParams
    ) throws ProcessException {

        try {
            httpNotifier.notify(callbackUrl, getWpsUrl(), getId(), callbackParams);
            return response;
        }
        catch (IOException e) {
            logger.error("Error sending notification", e);
            throw new ProcessException(e);
        }
    }

    URL getWpsUrl() throws MalformedURLException {
        // TODO is there a nicer way of getting BaseUrl?
        Dispatcher.REQUEST.get().getOperation();
        Operation op = Dispatcher.REQUEST.get().getOperation();
        ExecuteType execute = (ExecuteType) op.getParameters()[0];
        return new URL(execute.getBaseUrl() + "/ows");
    }

    String getId() {
        return resourceManager.getExecutionId(true);
    }
}
