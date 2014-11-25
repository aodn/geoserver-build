/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import au.org.emii.geoserver.extensions.filters.layer.data.LayerDataAccessor;
import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import au.org.emii.geoserver.extensions.filters.layer.data.FilterPropertyCheckBox;
import au.org.emii.geoserver.extensions.filters.layer.data.FilterPropertyTextField;
import au.org.emii.geoserver.extensions.filters.layer.data.io.LayerPropertiesReader;
import au.org.emii.geoserver.extensions.filters.layer.data.io.LayerPropertiesReaderFactory;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
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

        LayerPropertiesReader reader = LayerPropertiesReaderFactory.getReader(accessor.getDataSource(), accessor.getLayerIdentifier());
        add(new ListView<Filter>("layerDataProperties", reader.read())
        {
            public void populateItem(final ListItem<Filter> item)
            {
                final Filter filter = item.getModelObject();
                item.add(new FilterPropertyCheckBox("propertyEnabled", String.format("%s_enabled", filter.getName())));
                item.add(new Label("propertyName", filter.getName()));
                item.add(new Label("propertyType", filter.getType()));
                item.add(new FilterPropertyTextField("propertyLabel", filter));
                item.add(new FilterPropertyCheckBox("propertyWmsFilter", String.format("%s_wms_filter", filter.getName())));
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
