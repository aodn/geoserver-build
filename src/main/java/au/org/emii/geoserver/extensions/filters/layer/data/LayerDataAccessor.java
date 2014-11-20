/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.apache.wicket.model.IModel;

import javax.sql.DataSource;

public class LayerDataAccessor implements IModel<LayerDataAccessor> {

    private DataSource dataSource;
    private LayerIdentifier layerIdentifier;

    public LayerDataAccessor(DataSource dataSource, LayerIdentifier layerIdentifier) {
        this.dataSource = dataSource;
        this.layerIdentifier = layerIdentifier;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public LayerIdentifier getLayerIdentifier() {
        return layerIdentifier;
    }

    public LayerDataAccessor getObject() {
        return this;
    }

    public void setObject(LayerDataAccessor accessor) {}

    public void detach() {}
}
