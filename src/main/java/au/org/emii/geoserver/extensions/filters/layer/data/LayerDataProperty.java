/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

public class LayerDataProperty implements Serializable {

    private String name;
    private String type;

    public LayerDataProperty(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return StringUtils.capitalize(getName().replaceAll("_", " "));
    }
}
