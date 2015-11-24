package au.org.emii.wps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.opengis.wps10.ExecuteType;

import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.Operation;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.resource.WPSResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.emii.notifier.HttpNotifier;

public abstract class AbstractNotifierProcess implements GeoServerProcess {
    private final WPSResourceManager resourceManager;
    private final HttpNotifier httpNotifier;
    private static final Logger logger = LoggerFactory.getLogger(AbstractNotifierProcess.class);

    protected AbstractNotifierProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier) {
        this.resourceManager = resourceManager;
        this.httpNotifier = httpNotifier;
    }

    protected void notify(URL callbackUrl, String callbackParams) {
        if (callbackUrl == null) return;

        try {
            httpNotifier.notify(callbackUrl, getWpsUrl(), getId(), callbackParams);
        } catch (IOException ioe) {
            logger.error("Could not call callback", ioe);
        }
    }

    protected URL getWpsUrl() throws MalformedURLException {
        return new URL(getBaseUrl() + "ows");
    }

    protected String getBaseUrl() {
        // TODO is there a nicer way of getting BaseUrl?
        Dispatcher.REQUEST.get().getOperation();
        Operation op = Dispatcher.REQUEST.get().getOperation();
        ExecuteType execute = (ExecuteType) op.getParameters()[0];
        return execute.getBaseUrl();
    }


    protected String getId() {
        return resourceManager.getExecutionId(true);
    }

    protected WPSResourceManager getResourceManager() {
        return resourceManager;
    }

    protected String getWorkingDir() {
        try {
             // Use WPSResourceManager to create a temporary directory that will get cleaned up
             // when the process has finished executing (Hack! Should be a method on the resource manager)
             return resourceManager.getTemporaryResource("").dir().getAbsolutePath();
        } catch (Exception e) {
             logger.info("Exception accessing working directory: \n" + e);
             return System.getProperty("java.io.tmpdir");
        }
    }

}
