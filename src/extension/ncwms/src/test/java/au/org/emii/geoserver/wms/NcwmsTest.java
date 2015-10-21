package au.org.emii.geoserver.wms;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NcwmsTest extends TestCase {

    private NcwmsStyle getNcwmsStyle() throws Exception {

        SAXReader reader = new SAXReader();
        Document doc = reader.read(new FileInputStream("./src/test/resources/get_capabilities.xml"));

        return Ncwms.getStyles(doc, "sea_water_velocity");
    }

    public void testGetSupportedStyles() throws Exception {
        List<String> expectedStyles = new ArrayList<String>() {{
            add("barb");
            add("fancyvec");
            add("trivec");
            add("stumpvec");
            add("linevec");
            add("vector");
            add("boxfill");
        }};
        Collections.sort(expectedStyles);

        List<String> returnedStyles = Ncwms.getSupportedStyles(getNcwmsStyle());
        Collections.sort(returnedStyles);

        assertEquals(expectedStyles, returnedStyles);
    }

    public void testGetPalettes() throws Exception {
        List<String> expectedPalettes = new ArrayList<String>() {{
            add("alg");
            add("redblue");
            add("alg2");
            add("ncview");
            add("greyscale");
            add("occam");
            add("rainbow");
            add("sst_36");
            add("occam_pastel-30");
            add("ferret");
        }};
        Collections.sort(expectedPalettes);

        List<String> returnedPalettes = Ncwms.getPalettes(getNcwmsStyle());
        Collections.sort(returnedPalettes);

        assertEquals(expectedPalettes, returnedPalettes);
    }
}
