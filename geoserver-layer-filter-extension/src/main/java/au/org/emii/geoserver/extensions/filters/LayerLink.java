/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.Component;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

public class LayerLink extends LayerPageLink {

    public LayerLink(String id, LayerInfoModels models) {
        super(id, models);
    }

    public Component getLink() {
        return new SimpleBookmarkableLink(
            id,
            LayerFilterConfigurationPage.class,
            models.getName(),
            DataAccessEditPage.WS_NAME,
            models.getWorkspaceName(),
            DataAccessEditPage.STORE_NAME,
            models.getStoreName(),
            LayerFilterConfigurationPage.NAME,
            models.getNameName()
        );
    }
}
