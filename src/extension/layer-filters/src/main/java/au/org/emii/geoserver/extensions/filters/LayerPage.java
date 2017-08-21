/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class LayerPage extends GeoServerSecuredPage {

    WmsLayerFilterProvider provider = new WmsLayerFilterProvider();
    GeoServerTablePanel<LayerInfo> table;
    GeoServerDialog dialog;

    public LayerPage() {
        table = new GeoServerTablePanel<LayerInfo>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(
                String id,
                IModel itemModel,
                GeoServerDataProvider.Property<LayerInfo> property)
            {
                return LayerPageLink.create(property.getName(), id, new LayerInfoModels(itemModel)).getLink();
            }

        };
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    @Override
    protected String getTitle() {
        return new ParamResourceModel("au.org.emii.geoserver.extensions.filters.LayerPage.page.title", this).getString();
    }

    @Override
    protected String getDescription() {
        return new ParamResourceModel("au.org.emii.geoserver.extensions.filters.LayerPage.page.description", this).getString();
    }
}
