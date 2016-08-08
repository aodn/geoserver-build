package au.org.emii.utils;


import au.org.emii.core.Config;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GoGoDuckConfig extends Config {

    private final String GOGODUCK_CONFIG_FILE = "gogoduck.xml";
    private final String DEFAULT_CONFIG_FILE = "wps/gogoduck.xml";
    private final String FILE_PREFIX = "/gogoduck/";
    private final String GLOBAL_ATTRIBUTE_PREFIX = FILE_PREFIX + "globalAttributes/";
    private final String VARIABLES_TO_INCLUDE = FILE_PREFIX + "variablesToInclude/variable";

    private final String FILE_LIMIT_KEY = FILE_PREFIX + "fileLimit";
    private final String THREAD_COUNT_KEY = FILE_PREFIX + "threadCount";
    private final String URL_SUBSTITUTION = FILE_PREFIX + "urlSubstitution";

    private final String NCKS_PATH = FILE_PREFIX + "ncksPath";
    private final String NCPDQ_PATH = FILE_PREFIX + "ncpdqPath";
    private final String NCRCAT_PATH = FILE_PREFIX + "ncrcatPath";
    private final String NCATTED_PATH = FILE_PREFIX + "ncattedPath";

    private final String TITLE = GLOBAL_ATTRIBUTE_PREFIX + "title";
    private final String LATITUDE_START = GLOBAL_ATTRIBUTE_PREFIX + "latitudeStart";
    private final String LATITUDE_END = GLOBAL_ATTRIBUTE_PREFIX + "latitudeEnd";
    private final String LONGITUDE_START = GLOBAL_ATTRIBUTE_PREFIX + "longitudeStart";
    private final String LONGITUDE_END = GLOBAL_ATTRIBUTE_PREFIX + "longitudeEnd";
    private final String TIME_START = GLOBAL_ATTRIBUTE_PREFIX + "timeStart";
    private final String TIME_END = GLOBAL_ATTRIBUTE_PREFIX + "timeEnd";

    private final String UNPACK_NETCDF = FILE_PREFIX + "unpack";
    private final String TIME_FIELD = FILE_PREFIX + "timeField";
    private final String FILE_URL_FIELD = FILE_PREFIX + "fileUrlField";

    @Override
    public String getDefaultConfigFile() {
        return DEFAULT_CONFIG_FILE;
    }

    public GoGoDuckConfig(File resourceDirectory, Catalog catalog) {
        super(resourceDirectory, catalog);
    }

    public int getFileLimit() {
        return Integer.parseInt(getConfig(FILE_LIMIT_KEY, DEFAULT_CONFIG_FILE));
    }

    public int getThreadCount() {
        return Integer.parseInt(getConfig(THREAD_COUNT_KEY, DEFAULT_CONFIG_FILE));
    }

    public String getNcksPath() {
        return getConfig(NCKS_PATH, DEFAULT_CONFIG_FILE);
    }

    public String getNcpdqPath() {
        return getConfig(NCPDQ_PATH, DEFAULT_CONFIG_FILE);
    }

    public String getNcrcatPath() {
        return getConfig(NCRCAT_PATH, DEFAULT_CONFIG_FILE);
    }

    public String getNcattedPath() {
        return getConfig(NCATTED_PATH, DEFAULT_CONFIG_FILE);
    }

    public String getTimeField() {
        return getConfig(TIME_FIELD, DEFAULT_CONFIG_FILE);
    }

    public String getFileUrlField() {
        return getConfig(FILE_URL_FIELD, DEFAULT_CONFIG_FILE);
    }

    public Map<String, String> getUrlSubstitution(String layer) throws Exception {
        return getConfigMap(URL_SUBSTITUTION, getLayerConfigFilePath(layer));
    }

    public List<String> getVariablesToInclude(String layer) throws Exception {
        List<String> cmdConfigParameters = new ArrayList<>();
        List<String> configList = getConfigList(VARIABLES_TO_INCLUDE, getLayerConfigFilePath(layer));
        if (configList.size() > 0) {
            String commaSeparatedConfig = StringUtils.join(configList, ',');
            cmdConfigParameters.add("-v");
            cmdConfigParameters.add(commaSeparatedConfig);
        }
        return cmdConfigParameters;
    }

    public String getTitle(String layer) throws Exception {
        return getConfig(TITLE, getLayerConfigFilePath(layer));
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

    public boolean getUnpack(String layer) throws Exception {
        return Boolean.valueOf(getConfig(UNPACK_NETCDF, getLayerConfigFilePath(layer)));
    }

    private String getLayerConfigFilePath(String layer) throws Exception {
        return getLayerConfigPath(layer, GOGODUCK_CONFIG_FILE);
    }
}
