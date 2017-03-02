package au.org.emii.aggregator.template;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ValueTemplateTest {
    private Map<String, String> valueMap;

    @Before
    public void setup() {
        valueMap = new HashMap<>();
        valueMap.put("emptyString", "");
        valueMap.put("var1", "value1");
        valueMap.put("var2", "value2");
        valueMap.put("var4", null);
        valueMap.put("time_coverage_start", "2016-09-10T14:30:00Z");
        valueMap.put("time_coverage_end", "2016-10-19T14:30:00Z");
    }

    @Test
    public void noReplacementsToBeMade() {
        ValueTemplate valueTemplate = new ValueTemplate("value");
        assertEquals("value", valueTemplate.getValue(valueMap));
    }

    @Test
    public void replacingVariables() {
        ValueTemplate valueTemplate = new ValueTemplate(
            "empty string: ${emptyString}, var3: ${var3}, var2: ${var2}, var4: ${var4}"
        );
        assertEquals("empty string: , var3: ${var3}, var2: value2, var4: ${var4}", valueTemplate.getValue(valueMap));
    }

    @Test
    public void captureGroups() {
        ValueTemplate valueTemplate = new ValueTemplate(
            Pattern.compile("(.*?)(,[^,]*)?"),
            "${1}, ${time_coverage_start}, ${time_coverage_end}"
        );
        assertEquals("One hour averaged current QC data, 2016-09-10T14:30:00Z, 2016-10-19T14:30:00Z",
            valueTemplate.getValue("One hour averaged current QC data, 2016-09-12T14:30:00Z", valueMap));
        assertEquals("One hour averaged current QC data, 2016-09-10T14:30:00Z, 2016-10-19T14:30:00Z",
            valueTemplate.getValue("One hour averaged current QC data", valueMap));
        assertEquals("One hour averaged, current QC data, 2016-09-10T14:30:00Z, 2016-10-19T14:30:00Z",
            valueTemplate.getValue("One hour averaged, current QC data, 2016-09-12T14:30:00Z", valueMap));
    }

    @Test
    public void noPatternMatch() {
        ValueTemplate valueTemplate = new ValueTemplate(
            Pattern.compile("xxx"),
            "${1}${time_coverage_start} - ${time_coverage_end}"
        );
        assertEquals("One hour averaged current QC data, 2016-09-12T14:30:00Z",
            valueTemplate.getValue("One hour averaged current QC data, 2016-09-12T14:30:00Z", valueMap));
    }
}