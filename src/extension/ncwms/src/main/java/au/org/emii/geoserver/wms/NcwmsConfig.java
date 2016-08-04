package au.org.emii.geoserver.wms;

import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.dom4j.xpath.DefaultXPath;
import org.geoserver.platform.GeoServerResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NcwmsConfig {
    static Logger logger = LoggerFactory.getLogger(NcwmsConfig.class);

    public final static String CONFIG_FILE = "ncwms.xml";

    private final GeoServerResourceLoader resourceLoader;

    public NcwmsConfig(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private String getConfigFile() {
        return FilenameUtils.concat(resourceLoader.getBaseDirectory().toString(), CONFIG_FILE);
    }

    public String getConfigVariable(String xpathString, String defaultValue) {
        SAXReader reader = new SAXReader();
        String returnValue = defaultValue;

        try {
            Document doc = reader.read(getConfigFile());
            DefaultXPath xpath = new DefaultXPath(xpathString);
            returnValue = xpath.selectSingleNode(doc).getText();
        }
        catch (DocumentException e) {
            logger.warn(String.format("Could not open config file '%s': '%s'", getConfigFile(), e.getMessage()));
        }

        return returnValue;
    }

    public Map<String, String> getConfigMap(String xpathString) {
        SAXReader reader = new SAXReader();

        Map<String, String> returnValue = new HashMap<>();

        try {
            Document doc = reader.read(getConfigFile());
            DefaultXPath xpath = new DefaultXPath(xpathString);

            @SuppressWarnings("unchecked")
            List<DefaultElement> list = xpath.selectNodes(doc);

            for (final DefaultElement element : list) {
                returnValue.put(element.attribute("key").getText(), element.getText());
            }
        }
        catch (DocumentException e) {
            logger.warn(String.format("Could not open config file '%s': '%s'", getConfigFile(), e.getMessage()));
        }

        return returnValue;
    }

    public List<String> getConfigList(String xpathString) {
        SAXReader reader = new SAXReader();

        List<String> returnValue = new ArrayList<>();

        try {
            Document doc = reader.read(getConfigFile());
            DefaultXPath xpath = new DefaultXPath(xpathString);

            @SuppressWarnings("unchecked")
            List<DefaultElement> list = xpath.selectNodes(doc);

            for (final DefaultElement element : list) {
                returnValue.add(element.getText());
            }
        } catch (DocumentException e) {
            logger.warn( String.format("Could not read '%s' as an xml document: '%s'", getConfigFile(), e.getMessage()));
        } catch (ClassCastException e) {
            logger.warn(String.format("Error reading configuration file %s: '%s' does not return a list of elements", getConfigFile(), xpathString));
        }

        return returnValue;
    }
}
