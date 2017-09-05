package au.org.emii.wps;

import au.org.emii.notifier.HttpNotifier;
import au.org.emii.wps.catalogue.CatalogueReader;
import net.opengis.wps10.ExecuteType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.resource.WPSResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class AbstractNotifierProcess implements GeoServerProcess {
    private final WPSResourceManager resourceManager;
    private final HttpNotifier httpNotifier;
    private static final Logger logger = LoggerFactory.getLogger(AbstractNotifierProcess.class);
    private final GeoServer geoserver;
    private final CatalogueReader metadataCatalogue;

    protected AbstractNotifierProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier, GeoServer geoserver,
                                      CatalogueReader metadataCatalogue) {
        this.resourceManager = resourceManager;
        this.httpNotifier = httpNotifier;
        this.geoserver = geoserver;
        this.metadataCatalogue = metadataCatalogue;
    }

    protected void notifySuccess(URL callbackUrl, String callbackParams) {
        boolean successful = true;
        notify(successful, callbackUrl, callbackParams);
    }

    protected void notifyFailure(URL callbackUrl, String callbackParams) {
        boolean successful = false;
        notify(successful, callbackUrl, callbackParams);
    }

    protected void notify(boolean successful, URL callbackUrl, String callbackParams) {
        if (callbackUrl == null) return;

        try {
            httpNotifier.notify(callbackUrl, getWpsUrl(), getId(), successful, callbackParams);
        } catch (IOException ioe) {
            logger.error("Could not call callback", ioe);
        }
    }

    protected URL getWpsUrl() throws MalformedURLException {
        return new URL(ResponseUtils.appendPath(getBaseUrl(), "ows"));
    }

    protected String getBaseUrl() {
        String url = ((geoserver != null) ? geoserver.getSettings().getProxyBaseUrl() : null);

        if (url == null) {
            Operation op = Dispatcher.REQUEST.get().getOperation();
            ExecuteType execute = (ExecuteType) op.getParameters()[0];
            url = execute.getBaseUrl();
        }

        return url;
    }

    protected String getOutputResourceUrl(String outputId, String extension, String mimeType) {
        return resourceManager.getOutputResourceUrl(outputId + "." + extension, mimeType);
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

    protected String getMetadataUrl(String layer) {
        return metadataCatalogue.getMetadataUrl(layer);
    }

}
