package au.org.emii.gogoduck.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class GoGoDuckConfig {

    private static final Logger logger = LoggerFactory.getLogger(GoGoDuckConfig.class);
    private static final String PROPERTIES_FILE = "config.properties";
    private static Properties properties = new Properties();
    private static InputStream input = null;

    public static final String CONFIG_FILE = "wps/gogoduck.xml";
    public static final String FILE_LIMIT_KEY = "/gogoduck/fileLimit";
    public static final String THREAD_COUNT_KEY = "/gogoduck/threadCount";
    public static final String FILE_LIMIT_DEFAULT = "10";
    public static final String THREAD_COUNT_DEFAULT = "1";


    public static final String ncksPath = "/usr/bin/ncks";
    public static final String ncpdqPath = "/usr/bin/ncpdq";
    public static final String ncrcatPath = "/usr/bin/ncrcat";

    public static Properties getProperties() {
        if (properties.size() == 0) {
            loadProperties();
        }
        return  properties;
    }

    private static void loadProperties() {
        try {
            input = FeatureSourceIndexReader.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);

            if(input==null){
                throw new GoGoDuckException(String.format("Sorry, unable to find %s", PROPERTIES_FILE));
            }
            // load a properties file
            properties.load(input);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GoGoDuckException(String.format("Unable to load property file %s", PROPERTIES_FILE));
        } finally{
            if(input!=null){
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    public static Object getPropertyKeyByValue(String searchValue) {
        Object keyObject = null;
        Enumeration e = getProperties().propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (searchValue.equals(getProperties().getProperty(key))) {
                return key;
            }
        }
        return keyObject;
    }

    public static Object getPropertyKeyByValuePart(String searchValue) {
        String lastMatchKey = null;
        Enumeration e = getProperties().propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (searchValue.contains(getProperties().getProperty(key))) {
                // Finding the best match Ex: srs and srs_oc - need to return srs_oc
                if (lastMatchKey == null || key.length() > lastMatchKey.length()) {
                    lastMatchKey = key;
                }
            }
        }
        return lastMatchKey;
    }
}
