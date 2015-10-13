/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class ValueEncoder {

    List<String> encode(Set values) {
        List<String> result = new ArrayList<String>();

        for (Object value : values) {
            result.add(encode(value));
        }

        return result;
    }

    String encode(Object value) {

        if (value instanceof java.util.Date) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            return df.format(value);
        }

        return String.valueOf(value);
    }
}
