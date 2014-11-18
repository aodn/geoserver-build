/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.util.value.ValueMap;

public class LayerFilterForm extends Form<ValueMap> {

    public LayerFilterForm(final String id) {
        super(id, new CompoundPropertyModel<ValueMap>(new ValueMap()));

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
