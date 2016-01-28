package au.org.emii.geoserver.wms;

public class LayerDescriptor {
    public final String layer;
    public final String workspace;
    public final String timeField;
    public final String urlField;
    public final String variable;

    public static final String defaultTimeField = "time";
    public static final String defaultUrlField = "file_url";

    LayerDescriptor(String layerString) {
        String layerAndWorkspace = layerString;
        if (layerString.contains("/")) {
            layerAndWorkspace = layerString.split("/")[0];
            variable = layerString.split("/")[1];
        } else {
            variable = null;
        }

        String layerInformation;
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

    @Override
    public String toString() {
        if (variable != null) {
            return String.format("%s/%s", geoserverName(), variable);
        } else {
            return geoserverName();
        }
    }
}
