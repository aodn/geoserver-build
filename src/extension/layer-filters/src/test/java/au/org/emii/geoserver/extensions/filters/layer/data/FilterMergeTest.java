/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FilterMergeTest {

    private List<Filter> leftFilters;
    private List<Filter> rightFilters;
    private List<Filter> merged;

    public void setupLeftFilters() {
        leftFilters = new ArrayList<Filter>();
        for (int i = 0; i < 10; i++) {
            Filter filter = new Filter(buildFilterName(i), "String");
            filter.setLabel(String.format("left filter %s", i));
            leftFilters.add(filter);
        }

        Filter filter = new Filter("TimeRange", "String");
        filter.setLabel("timerange test");
        filter.setExtrasField("ATESTFIELD", "thetestvalue");
        leftFilters.add(filter);
    }

    public void setupRightFilters() {
        rightFilters = new ArrayList<Filter>();
        for (int i = 0; i < 8; i++) {
            Filter filter = new Filter(buildFilterName(i), "String");
            filter.setLabel(String.format("right filter %s", i));
            rightFilters.add(filter);
        }
    }

    @Before
    public void merge() {
        setupLeftFilters();
        setupRightFilters();
        merged = FilterMerge.merge(leftFilters, rightFilters);
    }

    private String buildFilterName(int i) {
        return String.format("filter %s", i);
    }

    @Test
    public void newListTest() {
        assertFalse(merged == leftFilters);
        assertFalse(merged == rightFilters);
    }

    @Test
    public void newListSizeEqualsLeftTest() {
        assertEquals(leftFilters.size(), merged.size());
    }

    @Test
    public void mergesRightSidePropertiesToTheLeftTest() {
        assertEquals(merged.get(0).getLabel(), rightFilters.get(0).getLabel());
    }

    @Test
    public void keepsLeftSideWhenNoMatchingRightSideTest() {
        assertEquals(merged.get(10).getLabel(), "timerange test");
    }
}
