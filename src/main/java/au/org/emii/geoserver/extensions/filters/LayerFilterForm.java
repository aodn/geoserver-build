/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import au.org.emii.geoserver.extensions.filters.layer.data.FilterConfiguration;
import au.org.emii.geoserver.extensions.filters.layer.data.io.FilterConfigurationFile;
import freemarker.template.TemplateException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.IOException;

public class LayerFilterForm extends Form<FilterConfiguration> {

    public LayerFilterForm(final String id, IModel<FilterConfiguration> model) {
        super(id, model);

        add(new ListView<Filter>("layerDataProperties", model.getObject().getFilters())
        {
            public void populateItem(final ListItem<Filter> item)
            {
                final Filter filter = item.getModelObject();
                item.add(new CheckBox("propertyEnabled", new PropertyModel<Boolean>(filter, "enabled")));
                item.add(new Label("propertyName", filter.getName()));
                item.add(new TextField<String>("propertyType", new Model<String>() {
                    @Override
                    public String getObject() {
                        return filter.getType();
                    }

                    @Override
                    public void setObject(final String value) {
                        filter.setType(value);
                    }
                }));
                item.add(new TextField<String>("propertyLabel", new Model<String>() {
                    @Override
                    public String getObject() {
                        return filter.getLabel();
                    }

                    @Override
                    public void setObject(final String value) {
                        filter.setLabel(value);
                    }
                }));
                item.add(new CheckBox("propertyWmsFilter", new Model<Boolean>() {
                    @Override
                    public Boolean getObject() {
                        return new Boolean(!filter.getVisualised().booleanValue());
                    }

                    @Override
                    public void setObject(Boolean value) {
                        filter.setVisualised(new Boolean(!value.booleanValue()));
                    }
                }));
                item.add(new CheckBox("propertyWfsExcluded", new PropertyModel<Boolean>(filter, "excludedFromDownload")));
            }
        });

        add(saveLink());
        add(cancelLink());
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                try {
                    FilterConfiguration data = getModel().getObject();
                    FilterConfigurationFile configurationFile = new FilterConfigurationFile(data.getDataDirectory());
                    configurationFile.write(data.getFilters());
                }
                catch (TemplateException te) {
                    throw new RuntimeException(te);
                }
                catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        };
    }

    private Link cancelLink() {
        return new Link("cancel") {

            @Override
            public void onClick() {
                throw new org.apache.wicket.RestartResponseException(LayerPage.class);
            }
        };
    }
}
