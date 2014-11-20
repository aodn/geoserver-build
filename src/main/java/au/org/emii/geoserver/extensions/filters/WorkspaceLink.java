/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.Component;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

public class WorkspaceLink extends LayerPageLink {

    public WorkspaceLink(String id, LayerInfoModels models) {
        super(id, models);
    }

    public Component getLink() {
        return new SimpleBookmarkableLink(
            id,
            WorkspaceEditPage.class,
            models.getWorkspace(),
            "name",
            models.getWorkspaceName()
        );
    }
}
