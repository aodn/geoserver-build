
package au.org.emii.ncdfgenerator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;


public class XMLDefinitionTest {
    @Before
    public void before() {
    }

    @Test
    public void testStringLiteral() throws Exception {
        InputStream config = getClass().getResourceAsStream("/anmn_timeseries_gg.xml");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
        Node node = document.getFirstChild();

        NcdfDefinition definition = new NcdfDefinitionXMLParser().parse(node) ;
        assertTrue(definition != null);
    }
}

