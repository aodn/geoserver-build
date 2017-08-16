package au.org.emii.wps;

import au.org.emii.notifier.HttpNotifier;
import net.opengis.wps10.ExecuteType;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class AbstractNotifierProcess implements GeoServerProcess {
    private final WPSResourceManager resourceManager;
    private final HttpNotifier httpNotifier;
    private static final Logger logger = LoggerFactory.getLogger(AbstractNotifierProcess.class);
    private final GeoServer geoserver;
    private final String metadataProtocol;
    private final String geonetworkSearchString;


    protected AbstractNotifierProcess(WPSResourceManager resourceManager, HttpNotifier httpNotifier, GeoServer geoserver) {
        this.resourceManager = resourceManager;
        this.httpNotifier = httpNotifier;
        this.geoserver = geoserver;
        this.metadataProtocol = "WWW:LINK-1.0-http--metadata-URL";

        // todo load from config
        this.geonetworkSearchString = "https://catalogue-portal.aodn.org.au/geonetwork/srv/eng/xml.search.summary?any=%s&sortBy=relevance&hitsPerPage=1&fast=index";
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

    protected String getMetadataUrl(String layer) throws ProcessException {

        String url = String.format(geonetworkSearchString, layer);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(new URL(url).openStream());

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//metadata/link");
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < nl.getLength(); i++) {
                String nodeValue = nl.item(i).getTextContent();
                if (nodeValue.contains(metadataProtocol)) {
                    return nodeValue.split("\\|")[2];
                }
            }
        }
        catch (Exception e) {
            throw new ProcessException("Unable to get metadata Url");
        }

        throw new ProcessException("Unable to parse metadata Url");

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
