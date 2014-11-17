/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.layer.LayerProvider;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.WMSStoreEditPage;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.*;

import static au.org.emii.geoserver.extensions.filters.LayerInfoProperties.*;

public class LayerPage extends GeoServerSecuredPage {

    WmsLayerFilterProvider provider = new WmsLayerFilterProvider();
    GeoServerTablePanel<LayerInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public LayerPage() {
        table = new GeoServerTablePanel<LayerInfo>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(
                String id,
                IModel itemModel,
                GeoServerDataProvider.Property<LayerInfo> property)
            {
                if(property.getName().equals(WORKSPACE.getName())) {
                    return workspaceLink(id, itemModel);
                }
                else if(property.getName().equals(STORE.getName())) {
                    return storeLink(id, itemModel);
                }
                else if(property.getName().equals(NAME.getName())) {
                    return layerLink(id, itemModel);
                }
                throw new IllegalArgumentException("Don't know a property named " + property.getName());
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(table.getSelection().size() > 0);
                target.addComponent(removal);
            }

        };
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
    }

    private Component layerLink(String id, final IModel model) {
        IModel layerNameModel = NAME.getModel(model);
        String wsName = (String) WORKSPACE.getModel(model).getObject();
        String layerName = (String) layerNameModel.getObject();

        return new SimpleBookmarkableLink(
            id,
            ResourceConfigurationPage.class,
            layerNameModel,
            ResourceConfigurationPage.NAME,
            layerName,
            ResourceConfigurationPage.WORKSPACE,
            wsName
        );
    }

    private Component storeLink(String id, final IModel model) {
        IModel storeModel = STORE.getModel(model);
        String wsName = (String) WORKSPACE.getModel(model).getObject();
        String storeName = (String) storeModel.getObject();
        LayerInfo layer = (LayerInfo) model.getObject();
        StoreInfo store = layer.getResource().getStore();
        if(store instanceof DataStoreInfo) {
            return new SimpleBookmarkableLink(
                id,
                DataAccessEditPage.class,
                storeModel,
                DataAccessEditPage.STORE_NAME,
                storeName,
                DataAccessEditPage.WS_NAME,
                wsName
            );
        }
        else if (store instanceof WMSStoreInfo) {
            return new SimpleBookmarkableLink(
                id,
                WMSStoreEditPage.class,
                storeModel,
                DataAccessEditPage.STORE_NAME,
                storeName,
                DataAccessEditPage.WS_NAME,
                wsName
            );
        }
        else {
            return new SimpleBookmarkableLink(
                id,
                CoverageStoreEditPage.class,
                storeModel,
                DataAccessEditPage.STORE_NAME,
                storeName,
                DataAccessEditPage.WS_NAME,
                wsName
            );
        }
    }

    private Component workspaceLink(String id, final IModel model) {
        IModel nameModel = WORKSPACE.getModel(model);
        return new SimpleBookmarkableLink(
            id,
            WorkspaceEditPage.class,
            nameModel,
            "name",
            (String)nameModel.getObject()
        );
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
