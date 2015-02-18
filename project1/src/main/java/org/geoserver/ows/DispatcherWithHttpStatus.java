package org.geoserver.ows;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

public class DispatcherWithHttpStatus extends org.geoserver.ows.Dispatcher {

    /**
     * Logging instance
     */
    static Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.ows");

    void handleServiceException(ServiceException se, Service service, Request request) {
        try {
            request.getHttpResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (IOException e) {
            logger.log(Level.INFO, "Problem sending error", e);
        }
    }
}
