package org.geoserver.ows;

import javax.servlet.http.HttpServletResponse;

import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

public class DispatcherWithHttpStatus extends org.geoserver.ows.Dispatcher {

    void handleServiceException(ServiceException se, Service service, Request request) {
        super.handleServiceException(se, service, request);
        request.getHttpResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
