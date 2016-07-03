package au.org.emii.geoserver.wms;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class LayerDescriptorTest {
    @Test
    public void testSimpleParsing() throws Exception {
        LayerDescriptor layerDescriptor;

        layerDescriptor = new LayerDescriptor("imos:some_layer/sea_water_velocity");
        assertEquals("imos", layerDescriptor.workspace);
        assertEquals("some_layer", layerDescriptor.layer);
        assertEquals("sea_water_velocity", layerDescriptor.getNetCDFVariableName());
        assertEquals("imos:some_layer", layerDescriptor.geoserverName());
        assertEquals("imos:some_layer/sea_water_velocity", layerDescriptor.toString());
        assertEquals("time", layerDescriptor.getTimeFieldName());
        assertEquals("file_url", layerDescriptor.getUrlFieldName());

        layerDescriptor = new LayerDescriptor("some_layer/sea_water_velocity");
        assertEquals(null, layerDescriptor.workspace);
        assertEquals("some_layer", layerDescriptor.layer);
        assertEquals("sea_water_velocity", layerDescriptor.getNetCDFVariableName());
        assertEquals("some_layer", layerDescriptor.geoserverName());
        assertEquals("some_layer/sea_water_velocity", layerDescriptor.toString());
        assertEquals("time", layerDescriptor.getTimeFieldName());
        assertEquals("file_url", layerDescriptor.getUrlFieldName());
    }

    @Test
    public void testParsingWithFieldNames() throws Exception {
        LayerDescriptor layerDescriptor = new LayerDescriptor("imos:some_layer#var1,var2/sea_water_velocity");
        assertEquals("imos", layerDescriptor.workspace);
        assertEquals("some_layer", layerDescriptor.layer);
        assertEquals("sea_water_velocity", layerDescriptor.getNetCDFVariableName());
        assertEquals("imos:some_layer", layerDescriptor.geoserverName());
        assertEquals("imos:some_layer/sea_water_velocity", layerDescriptor.toString());
        assertEquals("var1", layerDescriptor.getTimeFieldName());
        assertEquals("var2", layerDescriptor.getUrlFieldName());
    }
}
