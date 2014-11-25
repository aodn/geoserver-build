/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.apache.wicket.markup.html.form.TextField;

public class FilterPropertyTextField extends TextField {

    private Filter layerProperty;

    public FilterPropertyTextField(String id, Filter layerProperty) {
        this(id);
        this.layerProperty = layerProperty;
    }

    public FilterPropertyTextField(String id) {
        super(id);
    }

    @Override
    public String getInputName() {
        return layerProperty.getName();
    }

    @Override
    protected String getModelValue() {
        return layerProperty.getLabel();
    }
}
