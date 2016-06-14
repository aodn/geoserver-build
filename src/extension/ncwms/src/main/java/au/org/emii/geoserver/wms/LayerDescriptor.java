package au.org.emii.geoserver.wms;

import org.geotools.util.logging.Logging;

import java.util.logging.Logger;

public class LayerDescriptor {
    static Logger LOGGER = Logging.getLogger(Ncwms.class);

    public final String layer;
    public final String workspace;
    public final String timeField;
    public final String urlField;
    public final String netCDFVariable;

    public static final String defaultTimeField = "time";
    public static final String defaultUrlField = "file_url";

    LayerDescriptor(String layerString) {
        if (!layerString.contains("/")) {
            throw new RuntimeException(String.format("Layer descriptor '%s' does not contain a '/'", layerString));
        }

        String layerAndWorkspace = layerString.split("/")[0];
        String layerInformation;
        netCDFVariable = layerString.split("/")[1];
        if (layerString.contains(":")) {
            workspace = layerAndWorkspace.split(":")[0];
            layerInformation = layerAndWorkspace.split(":")[1];
        }
        else {
            workspace = null;
            layerInformation = layerAndWorkspace;
        }

        if (layerInformation.contains("#")) {
            layer = layerInformation.split("#")[0];
            String fieldNames = layerInformation.split("#")[1];
            timeField = fieldNames.split(",")[0];
            urlField = fieldNames.split(",")[1];
        }
        else {
            layer = layerInformation;
            timeField = defaultTimeField;
            urlField = defaultUrlField;
        }
    }

    private String getWorkspace() {
        if (workspace == null)
            return "";
        else
            return workspace + ":";
    }

    public String geoserverName() {
        return String.format("%s%s", getWorkspace(), layer);
    }

    public String getTimeFieldName() {
        return timeField;
    }

    public String getUrlFieldName() {
        return urlField;
    }

    public String getNetCDFVariableName() { return netCDFVariable; }

    @Override
    public String toString() {
        return String.format("%s/%s", geoserverName(), netCDFVariable);
    }
}
