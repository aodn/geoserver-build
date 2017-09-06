package au.org.emii.wps.provenance;

import com.google.common.io.Files;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProvenanceWriterTest {
    @Test
    public void testWrite() throws IOException, TemplateException, URISyntaxException {
        Configuration config = new Configuration();
        config.setClassForTemplateLoading(ProvenanceWriter.class, "");
        ProvenanceWriter writer = new ProvenanceWriter(config);
        Map<String, Object> params = new HashMap<>();
        params.put("layer", "acorn");
        params.put("subset", "LAT;10;20");
        params.put("executionId", "0983210938098");
        String output = writer.write("test.xml", params);
        assertEquals(getResourceAsString("expected.xml"), output);
    }

    private String getResourceAsString(String resource) throws URISyntaxException, IOException {
        URL resourceUrl = ProvenanceWriter.class.getResource(resource);
        File file = new File(resourceUrl.toURI());
        return Files.toString(file, Charset.forName("UTF-8"));
    }

}
