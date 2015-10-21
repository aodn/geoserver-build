package au.org.emii.geoserver.wms;

public class LayerDescriptor {
    public final String groupLayerName;
    public final String workspace;
    public final String layerName;

    LayerDescriptor(String layerString) {
        String layerAndWorkspace = layerString.split("/")[0];
        layerName = layerString.split("/")[1];
        if (layerString.contains(":")) {
            workspace = layerAndWorkspace.split(":")[0];
            groupLayerName = layerAndWorkspace.split(":")[1];
        }
        else {
            workspace = null;
            groupLayerName = layerAndWorkspace;
        }
    }

    private String getWorkspace() {
        if (workspace == null) {
            return "";
        }
        else {
            return workspace + ":";
        }
    }

    public String geoserverName() {
        return String.format("%s%s", getWorkspace(), groupLayerName);
    }

    @Override
    public String toString() {
        return String.format("%s/%s", geoserverName(), layerName);
    }
}
