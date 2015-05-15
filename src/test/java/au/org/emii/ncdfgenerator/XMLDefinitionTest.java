package au.org.emii.ncdfgenerator;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class XMLDefinitionTest {
    @Before
    public void before() {
    }

    @Test
    public void testStringLiteral() throws Exception {
        InputStream config = getClass().getResourceAsStream("/anmn_ts.xml");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
        Node node = document.getFirstChild();

        NcdfDefinition definition = new NcdfDefinitionXMLParser().parse(node);
        assertTrue(definition != null);
    }
}

