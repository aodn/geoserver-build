/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import java.util.*;

public class FilterMerge {

    public static List<Filter> merge(List<Filter> leftList, List<Filter> rightList) {
        List<Filter> merged = new ArrayList<Filter>(leftList.size());
        Map<String, Filter> map = map(rightList);

        for (Filter left : leftList) {
            Filter right = map.get(left.getName());
            merged.add(left.merge(right));
        }

        return merged;
    }

    private static Map<String, Filter> map(List<Filter> filters) {
        Map<String, Filter> map = new HashMap<String, Filter>(filters.size());

        for (Filter filter : filters) {
            map.put(filter.getName(), filter);
        }

        return map;
    }
}
