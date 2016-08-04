package au.org.emii.utils;


import au.org.emii.core.Config;
import org.geoserver.catalog.Catalog;

import java.io.File;
import java.util.List;

public class GoGoDuckConfig extends Config {

    private final String GOGODUCK_CONFIG_FILE = "gogoduck.xml";
    private final String CONFIG_FILE = "wps/gogoduck.xml";
    private final String FILE_PREFIX = "/gogoduck/";

    private final String FILE_LIMIT_KEY = FILE_PREFIX + "fileLimit";
    private final String THREAD_COUNT_KEY = FILE_PREFIX + "threadCount";
    private final String URL_SUBSTITUTION = FILE_PREFIX + "urlSubstitution";

    private final String NCKS_PATH = FILE_PREFIX + "ncksPath";
    private final String NCPDQ_PATH = FILE_PREFIX + "ncpdqPath";
    private final String NCRCAT_PATH = FILE_PREFIX + "ncrcatPath";

    private final String NCKS_PARAMETERS = FILE_PREFIX + "variablesToInclude";
    private final String LATITUDE_START = FILE_PREFIX + "latitude.start";
    private final String LATITUDE_END = FILE_PREFIX + "latitude.end";
    private final String LONGITUDE_START = FILE_PREFIX + "longitude.start";
    private final String LONGITUDE_END = FILE_PREFIX + "longitude.end";
    private final String TIME_START = FILE_PREFIX + "time.start";
    private final String TIME_END = FILE_PREFIX + "time.end";
    private final String UNPACK_NETCDF = FILE_PREFIX + "unpack.netcdf";

    private final String TIME_FIELD = FILE_PREFIX + "time.field";
    private final String FILE_URL_FIELD = FILE_PREFIX + "file.url.field";

    public GoGoDuckConfig(File resourceDirectory, Catalog catalog) {
        super(resourceDirectory, catalog);
    }

    public int getFileLimit() {
        return Integer.parseInt(getConfig(FILE_LIMIT_KEY, CONFIG_FILE));
    }

    public int getThreadCount() {
        return Integer.parseInt(getConfig(THREAD_COUNT_KEY, CONFIG_FILE));
    }

    public String getUrlSubstitutionKey() {
        return getConfigKey(URL_SUBSTITUTION, CONFIG_FILE);
    }

    public String getUrlSubstitutionValue() {
        return getConfigValue(URL_SUBSTITUTION, CONFIG_FILE);
    }

    public String getNcksPath() {
        return getConfig(NCKS_PATH, CONFIG_FILE);
    }

    public String getNcpdqPath() {
        return getConfig(NCPDQ_PATH, CONFIG_FILE);
    }

    public String getNcrcatPath() {
        return getConfig(NCRCAT_PATH, CONFIG_FILE);
    }

    public String getTimeField() {
        return getConfig(TIME_FIELD, CONFIG_FILE);
    }

    public String getFileUrlField() {
        return getConfig(FILE_URL_FIELD, CONFIG_FILE);
    }

    public List<String> getNcksParameters(String layer) throws Exception {
        return getConfigList(NCKS_PARAMETERS, getLayerConfigFilePath(layer));
    }

    public String getLatitudeStart(String layer) throws Exception {
        return getConfig(LATITUDE_START, getLayerConfigFilePath(layer));
    }

    public String getLatitudeEnd(String layer) throws Exception {
        return getConfig(LATITUDE_END, getLayerConfigFilePath(layer));
    }

    public String getLongitudeStart(String layer) throws Exception {
        return getConfig(LONGITUDE_START, getLayerConfigFilePath(layer));
    }

    public String getLongitudeEnd(String layer) throws Exception {
        return getConfig(LONGITUDE_END, getLayerConfigFilePath(layer));
    }

    public String getTimeStart(String layer) throws Exception {
        return getConfig(TIME_START, getLayerConfigFilePath(layer));
    }

    public String getTimeEnd(String layer) throws Exception {
        return getConfig(TIME_END, getLayerConfigFilePath(layer));
    }

    public boolean getUnpackNetcdf(String layer) throws Exception {
        String config = getConfig(UNPACK_NETCDF, getLayerConfigFilePath(layer));
        if (config == null) {
            return false;
        } else {
            return Boolean.valueOf(config);
        }
    }

    public String getLayerConfigFilePath(String layer) throws Exception {
        return getLayerConfigPath(layer, GOGODUCK_CONFIG_FILE);
    }
}
