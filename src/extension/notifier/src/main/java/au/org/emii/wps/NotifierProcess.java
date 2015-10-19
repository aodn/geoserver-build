package au.org.emii.wps;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import net.opengis.wps10.ExecuteType;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.Operation;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DescribeProcess(title="Notifier", description="Notify subscribers when a WPS process completes")
public class NotifierProcess implements GeoServerProcess {
    private static final Logger logger = LoggerFactory.getLogger(NotifierProcess.class);

    private final WPSResourceManager resourceManager;
    private final HttpNotifier httpNotifier;
    private final int maxNotificationAttempts;
    private final int retryInterval;

    public NotifierProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier, int maxNotificationAttempts, int retryInterval) {
        this.resourceManager = resourceManager;
        this.httpNotifier = httpNotifier;
        this.maxNotificationAttempts = maxNotificationAttempts;
        this.retryInterval = retryInterval;
    }

    @DescribeResult(name="result", description="NetCDF file", meta={"mimeTypes=application/x-netcdf"})
    public RawData execute(
        @DescribeParameter(name="notifiable", description="NetCDF file")
        RawData notifiableData,
        @DescribeParameter(name="callbackUrl", description="Callback URL")
        URL callbackUrl,
        @DescribeParameter(name="callbackParams", description="Parameters to append to the callback")
        String callbackParams
    ) throws ProcessException {

        try {
            IOException lastException;
            int attempt = 1;

            do {
                try {
                    logger.debug("Notification attempt #" + attempt);
                    httpNotifier.notify(callbackUrl, getWpsUrl(), getId(), callbackParams);

                    return notifiableData;
                }
                catch (IOException e) {
                    logger.info("Attempt #" + attempt + " failed", e);
                    lastException = e;

                    try {
                        Thread.sleep(retryInterval);
                    }
                    catch (InterruptedException ie) {
                        logger.debug("Sleep interrupted", ie);
                    }
                }
            }
            while (attempt++ < maxNotificationAttempts);

            throw lastException;
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
