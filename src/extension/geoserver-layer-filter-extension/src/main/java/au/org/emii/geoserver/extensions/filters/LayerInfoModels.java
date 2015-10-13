/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;

import static au.org.emii.geoserver.extensions.filters.LayerInfoProperties.NAME;
import static au.org.emii.geoserver.extensions.filters.LayerInfoProperties.STORE;
import static au.org.emii.geoserver.extensions.filters.LayerInfoProperties.WORKSPACE;

public class LayerInfoModels {

    private IModel model;

    public LayerInfoModels(IModel model) {
        this.model = model;
    }

    public IModel getWorkspace() {
        return WORKSPACE.getModel(model);
    }

    public String getWorkspaceName() {
        return (String)getWorkspace().getObject();
    }

    public IModel getStore() {
        return STORE.getModel(model);
    }

    public String getStoreName() {
        return (String)getStore().getObject();
    }

    public IModel getName() {
        return NAME.getModel(model);
    }

    public String getNameName() {
        return (String)getName().getObject();
    }

    public LayerInfo getLayerInfo() {
        return (LayerInfo)model.getObject();
    }
}
