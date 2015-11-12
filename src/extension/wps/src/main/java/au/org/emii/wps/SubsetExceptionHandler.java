package au.org.emii.wps;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.wps.process.StringRawData;
import org.opengis.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsetExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(SubsetExceptionHandler.class);

    private final SubsetOperation wrappedOperation;

    public SubsetExceptionHandler(SubsetOperation wrappedOperation) {
        this.wrappedOperation = wrappedOperation;
    }

    public Map<String, Object> subset(String typeName, String filterExpression, ProgressListener progressListener) {
        Map<String, Object> outputs = new HashMap<String, Object>();

        try {
            outputs.put("result", wrappedOperation.subset(typeName, filterExpression, progressListener));
            outputs.put("errors", "");
        } catch (Exception e) {
            logger.error("Error performing subset", e);
            outputs.put("result", new StringRawData("", wrappedOperation.getResultType()));
            outputs.put("errors", getErrorString(e));
        }

        return outputs;
    }

    private String getErrorString(Exception e) {
        String exceptionMessage = e.getMessage();

        if (exceptionMessage == null || exceptionMessage.trim().isEmpty()) {
            return "Error performing subset";
        } else {
            return exceptionMessage;
        }
    }

}
