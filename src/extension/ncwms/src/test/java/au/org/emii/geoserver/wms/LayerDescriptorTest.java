package au.org.emii.geoserver.wms;

import junit.framework.TestCase;
import org.junit.Test;

public class LayerDescriptorTest extends TestCase {
    @Test
    public void testSimpleParsing() throws Exception {
        LayerDescriptor layerDescriptor;

        layerDescriptor = new LayerDescriptor("imos:some_layer/sea_water_velocity");
        assertEquals("imos", layerDescriptor.workspace);
        assertEquals("some_layer", layerDescriptor.layer);
        assertEquals("sea_water_velocity", layerDescriptor.variable);
        assertEquals("imos:some_layer", layerDescriptor.geoserverName());
        assertEquals("imos:some_layer/sea_water_velocity", layerDescriptor.toString());

        layerDescriptor = new LayerDescriptor("some_layer/sea_water_velocity");
        assertEquals(null, layerDescriptor.workspace);
        assertEquals("some_layer", layerDescriptor.layer);
        assertEquals("sea_water_velocity", layerDescriptor.variable);
        assertEquals("some_layer", layerDescriptor.geoserverName());
        assertEquals("some_layer/sea_water_velocity", layerDescriptor.toString());
    }
}
