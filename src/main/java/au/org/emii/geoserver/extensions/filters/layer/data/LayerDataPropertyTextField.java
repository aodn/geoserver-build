/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.apache.wicket.markup.html.form.TextField;

public class LayerDataPropertyTextField extends TextField {

    private LayerDataProperty layerProperty;

    public LayerDataPropertyTextField(String id, LayerDataProperty layerProperty) {
        this(id);
        this.layerProperty = layerProperty;
    }

    public LayerDataPropertyTextField(String id) {
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
