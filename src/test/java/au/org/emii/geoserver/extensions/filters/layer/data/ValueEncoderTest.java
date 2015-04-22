/*
 * Copyright 2015 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data;

import org.junit.Test;
import java.util.*;

import static org.junit.Assert.*;

public class ValueEncoderTest {

    @Test
    public void encodeValueAsStringTest() {
        Map<Object, String> expectedEncodings = new HashMap<Object, String>();
        expectedEncodings.put(Boolean.valueOf(true), "true");
        expectedEncodings.put(Integer.valueOf(123), "123");
        expectedEncodings.put(Long.valueOf(456l), "456");
        expectedEncodings.put(Float.valueOf(123.456f), "123.456");
        expectedEncodings.put(Double.valueOf(3.1415), "3.1415");
        expectedEncodings.put("i am a string", "i am a string");
        expectedEncodings.put(new java.sql.Date(3600000), "1970-01-01T01:00:00Z");
        expectedEncodings.put(new java.sql.Timestamp(7200000), "1970-01-01T02:00:00Z");

        for (Map.Entry<Object, String> encoding : expectedEncodings.entrySet()) {
            Object unencoded = encoding.getKey();
            String encoded = encoding.getValue();

            assertEquals(encoded, new ValueEncoder().encode(unencoded));
        }
    }
}
