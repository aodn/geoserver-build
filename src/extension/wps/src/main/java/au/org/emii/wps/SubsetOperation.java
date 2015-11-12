package au.org.emii.wps;

import org.geoserver.wps.process.RawData;
import org.opengis.util.ProgressListener;

public interface SubsetOperation {

    RawData subset(String typeName, String filterExpression,
            ProgressListener progressListener) throws Exception;

    String getResultType();

}
