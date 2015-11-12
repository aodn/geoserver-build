package au.org.emii.wps;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.geoserver.wps.ppio.XMLPPIO;
import org.geoserver.wps.process.StringRawData;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Decoder/encoder for 'text/xml' StringRawData
 *  
 */

public class StringRawDataXmlPPIO extends XMLPPIO {

    public StringRawDataXmlPPIO() {
        super(StringRawData.class, StringRawData.class, new QName("notUsed"));
   }

    @Override
    public Object decode(InputStream input) throws Exception {
        return new StringRawData(IOUtils.toString(input, null), mimeType);
    }

    @Override
    public void encode(Object object, ContentHandler handler) throws Exception {
        String xmlString = ((StringRawData) object).getData();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(handler);
        xmlReader.parse(new InputSource(new StringReader(xmlString)));
    }

}
