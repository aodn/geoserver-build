/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;

public class FilterConfigurationTest {

    @Test
    public void constructorTest() {
        FilterConfiguration filterConfiguration = new FilterConfiguration("", new ArrayList<Filter>());

        assertNotNull(filterConfiguration.getDataDirectory());
        assertNotNull(filterConfiguration.getFilters());
    }
}
