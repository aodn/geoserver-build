/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class FilterTest {

    @Test
    public void labelTest() {
        Filter filter = new Filter();
        filter.setName("time_coverage_start");

        assertEquals("Time coverage start", filter.getLabel());
    }

    @Test
    public void isEnabledGetEnabledTest() {
        Filter filter = new Filter();
        filter.setEnabled(Boolean.TRUE);

        assertTrue(filter.getEnabled());
        assertEquals(filter.getEnabled(), filter.isEnabled());
    }

    @Test
    public void isVisualisedGetVisualisedTest() {
        Filter filter = new Filter();
        filter.setVisualised(Boolean.TRUE);

        assertTrue(filter.getVisualised());
        assertEquals(filter.getVisualised(), filter.isVisualised());
    }

    @Test
    public void nameMergeLeftTest() {
        Filter left = new Filter();
        left.setName("foo");

        Filter right = new Filter();

        Filter filter = left.merge(right);
        assertEquals("foo", filter.getName());
    }

    @Test
    public void nameMergeRightTest() {
        Filter left = new Filter();
        left.setName("foo");

        Filter right = new Filter();
        right.setName("bar");

        Filter filter = left.merge(right);
        assertEquals("bar", filter.getName());
    }

    @Test
    public void labelMergeLeftTest() {
        Filter left = new Filter();
        left.setLabel("foo");

        Filter right = new Filter();

        Filter filter = left.merge(right);
        assertEquals("foo", filter.getLabel());
    }

    @Test
    public void labelMergeRightTest() {
        Filter left = new Filter();
        left.setLabel("foo");

        Filter right = new Filter();
        right.setLabel("bar");

        Filter filter = left.merge(right);
        assertEquals("bar", filter.getLabel());
    }

    @Test
     public void typeMergeLeftTest() {
        Filter left = new Filter();
        left.setType("foo");

        Filter right = new Filter();

        Filter filter = left.merge(right);
        assertEquals("foo", filter.getType());
    }

    @Test
    public void typeMergeRightTest() {
        Filter left = new Filter();
        left.setType("foo");

        Filter right = new Filter();
        right.setType("bar");

        Filter filter = left.merge(right);
        assertEquals("bar", filter.getType());
    }

    @Test
    public void enabledMergeLeftTest() {
        Filter left = new Filter();
        left.setEnabled(Boolean.TRUE);

        Filter right = new Filter();

        Filter filter = left.merge(right);
        assertTrue(filter.getEnabled());
    }

    @Test
    public void enabledMergeRightTest() {
        Filter left = new Filter();
        left.setEnabled(Boolean.FALSE);

        Filter right = new Filter();
        right.setEnabled(Boolean.TRUE);

        Filter filter = left.merge(right);
        assertTrue(filter.getEnabled());
    }

    @Test
    public void visualisedMergeLeftTest() {
        Filter left = new Filter();
        left.setVisualised(Boolean.TRUE);

        Filter right = new Filter();

        Filter filter = left.merge(right);
        assertTrue(filter.getVisualised());
    }

    @Test
    public void visualisedMergeRightTest() {
        Filter left = new Filter();
        left.setVisualised(Boolean.FALSE);

        Filter right = new Filter();
        right.setVisualised(Boolean.TRUE);

        Filter filter = left.merge(right);
        assertTrue(filter.getVisualised());
    }

    @Test
    public void nullSafeTest() {
        Filter left = new Filter();
        left.merge(null);
    }

    @Test
    public void mutationSafeTest() {
        Filter left = new Filter();
        Filter right = new Filter();
        Filter result = left.merge(right);

        assertTrue(result != left);
        assertTrue(result != right);
    }
}
