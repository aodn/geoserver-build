/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.geoserver.web.GeoServerSecuredPage;

public class LayerFilterConfigurationPage extends GeoServerSecuredPage {

    public static final String NAME = "name";
    public static final String WORKSPACE = "wsName";

    private String layerName;
    private String workspaceName;

    public LayerFilterConfigurationPage(PageParameters parameters) {
        this(parameters.getString(WORKSPACE), parameters.getString(NAME));
    }

    public LayerFilterConfigurationPage(String workspaceName, String layerName) {
        this.workspaceName = workspaceName;
        this.layerName = layerName;

        add(new LayerFilterForm("layerFilterForm"));
        add(CSSPackageResource.getHeaderContribution(LayerFilterConfigurationPage.class, "layer_filters.css"));
    }

    @Override
    protected String getTitle() {
        return String.format("%s - %s", workspaceName, layerName);
    }

    @Override
    protected String getDescription() {
        return String.format("Configuring filters for %s - %s", workspaceName, layerName);
    }
}
