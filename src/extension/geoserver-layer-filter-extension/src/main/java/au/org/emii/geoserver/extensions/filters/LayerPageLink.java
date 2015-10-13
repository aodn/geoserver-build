/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.Component;

import static au.org.emii.geoserver.extensions.filters.LayerInfoProperties.*;

public abstract class LayerPageLink {

    protected String id;
    protected LayerInfoModels models;

    public LayerPageLink(String id, LayerInfoModels models) {
        this.id = id;
        this.models = models;
    }

    public abstract Component getLink();

    public static LayerPageLink create(String name, String id, LayerInfoModels models) {
        if(name.equals(WORKSPACE.getName())) {
            return new WorkspaceLink(id, models);
        }
        else if(name.equals(STORE.getName())) {
            return new StoreLink(id, models);
        }
        else if(name.equals(NAME.getName())) {
            return new LayerLink(id, models);
        }
        throw new IllegalArgumentException("Don't know a property named " + name);
    }
}
