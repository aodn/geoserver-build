package org.geoserver.ows;

import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

public class DispatcherWithHttpStatus extends org.geoserver.ows.Dispatcher {

    void handleServiceException(ServiceException se, Service service, Request request) {
        super.handleServiceException(se, service, request);
        request.getHttpResponse().setStatus(500);
    }
}
