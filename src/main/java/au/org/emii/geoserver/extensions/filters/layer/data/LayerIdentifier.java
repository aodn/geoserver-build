/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

public class LayerIdentifier {

    private String layerName;
    private String schemaName;

    public LayerIdentifier(String layerName, String schemaName) {
        this.layerName = layerName;
        this.schemaName = schemaName;
    }

    public String getLayerName() {
        return layerName;
    }

    public String getSchemaName() {
        return schemaName;
    }
}
