/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

public class Filter implements Serializable {

    private String name;
    private String type;
    private String label;
    private Boolean enabled;
    private Boolean visualised;

    public Filter(String name, String type) {
        this.name = name;
        this.type = type;
        this.enabled = Boolean.FALSE;
        this.visualised = Boolean.TRUE;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        if (label != null) {
            return label;
        }
        return StringUtils.capitalize(getName().replaceAll("_", " "));
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean isEnabled() {
        return getEnabled();
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getVisualised() {
        return visualised;
    }

    public Boolean isVisualised() {
        return getVisualised();
    }

    public void setVisualised(Boolean visualised) {
        this.visualised = visualised;
    }
}
