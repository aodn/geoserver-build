/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import au.org.emii.geoserver.extensions.filters.layer.data.LayerDataAccessor;
import au.org.emii.geoserver.extensions.filters.layer.data.LayerDataProperty;
import au.org.emii.geoserver.extensions.filters.layer.data.LayerDataPropertyCheckBox;
import au.org.emii.geoserver.extensions.filters.layer.data.LayerDataPropertyTextField;
import au.org.emii.geoserver.extensions.filters.layer.data.reader.LayerDataPropertiesReader;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.ValueMap;

public class LayerFilterForm extends Form<ValueMap> {

    public LayerFilterForm(final String id, IModel<LayerDataAccessor> model) {
        super(id, new CompoundPropertyModel<ValueMap>(new ValueMap()));

        LayerDataAccessor accessor = model.getObject();

        LayerDataPropertiesReader reader = new LayerDataPropertiesReader(accessor.getDataSource(), accessor.getLayerIdentifier());
        add(new ListView<LayerDataProperty>("layerDataProperties", reader.read())
        {
            public void populateItem(final ListItem<LayerDataProperty> item)
            {
                final LayerDataProperty layerProperty = item.getModelObject();
                item.add(new LayerDataPropertyCheckBox("propertyEnabled", String.format("%s_enabled", layerProperty.getName())));
                item.add(new Label("propertyName", layerProperty.getName()));
                item.add(new Label("propertyType", layerProperty.getType()));
                item.add(new LayerDataPropertyTextField("propertyLabel", layerProperty));
                item.add(new LayerDataPropertyCheckBox("propertyWmsFilter", String.format("%s_wms_filter", layerProperty.getName())));
            }
        });

        add(saveLink());
        add(cancelLink());
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                //doSave();
            }
        };
    }

    private Link cancelLink() {
        return new Link("cancel") {

            @Override
            public void onClick() {
                //onCancel();
            }
        };
    }
}
