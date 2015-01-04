/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.Component;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.WMSStoreEditPage;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

public class StoreLink extends LayerPageLink {

    public StoreLink(String id, LayerInfoModels models) {
        super(id, models);
    }

    public Component getLink() {
        return new StoreInfoLinkBuilder().build(getLayerInfo().getResource().getStore());
    }

    private LayerInfo getLayerInfo() {
        return models.getLayerInfo();
    }

    class StoreInfoLinkBuilder {
        public Component build(StoreInfo storeInfo) {
            return getComponent(CoverageStoreEditPage.class);
        }

        public Component build(DataStoreInfo storeInfo) {
            return getComponent(DataAccessEditPage.class);
        }

        public Component build(WMSStoreInfo storeInfo) {
            return getComponent(WMSStoreEditPage.class);
        }

        private Component getComponent(Class clazz) {
            return new SimpleBookmarkableLink(
                id,
                clazz,
                models.getStore(),
                DataAccessEditPage.STORE_NAME,
                models.getStoreName(),
                DataAccessEditPage.WS_NAME,
                models.getWorkspaceName()
            );
        }
    }
}
