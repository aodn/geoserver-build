package au.org.emii.geoserver.wms;

public class LayerDescriptor {
    public final String layer;
    public final String workspace;
    public final String variable;
    LayerDescriptor(String layerString) {
        String layerAndWorkspace = layerString.split("/")[0];
        variable = layerString.split("/")[1];
        if (layerString.contains(":")) {
            workspace = layerAndWorkspace.split(":")[0];
            layer = layerAndWorkspace.split(":")[1];
        }
        else {
            workspace = null;
            layer = layerAndWorkspace;
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

    @Override
    public String toString() {
        return String.format("%s/%s", geoserverName(), variable);
    }
}
