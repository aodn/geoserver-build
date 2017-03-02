package au.org.emii.wps.gogoduck.config;


import au.org.emii.aggregator.template.ValueTemplate;
import au.org.emii.core.Config;
import au.org.emii.wps.gogoduck.exception.GoGoDuckException;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class GoGoDuckConfig extends Config {

    private final String GOGODUCK_CONFIG_FILE = "gogoduck.xml";
    private final String DEFAULT_CONFIG_FILE = "wps/gogoduck.xml";
    private final String FILE_PREFIX = "/gogoduck/";
    private final String GLOBAL_ATTRIBUTE_PREFIX = FILE_PREFIX + "globalAttributes/";
    private final String VARIABLES_TO_INCLUDE = FILE_PREFIX + "variablesToInclude/variable";

    private final String FILE_LIMIT_KEY = FILE_PREFIX + "fileLimit";
    private final String URL_SUBSTITUTION = FILE_PREFIX + "urlSubstitution";

    private final String TITLE = GLOBAL_ATTRIBUTE_PREFIX + "title";
    private final String LATITUDE_START = GLOBAL_ATTRIBUTE_PREFIX + "latitudeStart";
    private final String LATITUDE_END = GLOBAL_ATTRIBUTE_PREFIX + "latitudeEnd";
    private final String LONGITUDE_START = GLOBAL_ATTRIBUTE_PREFIX + "longitudeStart";
    private final String LONGITUDE_END = GLOBAL_ATTRIBUTE_PREFIX + "longitudeEnd";
    private final String TIME_START = GLOBAL_ATTRIBUTE_PREFIX + "timeStart";
    private final String TIME_END = GLOBAL_ATTRIBUTE_PREFIX + "timeEnd";

    private final String TIME_FIELD = FILE_PREFIX + "timeField";
    private final String SIZE_FIELD = FILE_PREFIX + "sizeField";
    private final String FILE_SIZE_LIMIT = FILE_PREFIX + "fileSizeLimit";
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

    public String getTimeField() {
        return getConfig(TIME_FIELD, DEFAULT_CONFIG_FILE);
    }

    public String getSizeField() {
        return getConfig(SIZE_FIELD, DEFAULT_CONFIG_FILE);
    }

    public double getFileSizeLimit() {
        String limit = getConfig(FILE_SIZE_LIMIT, DEFAULT_CONFIG_FILE);
        if (StringUtils.isNotEmpty(limit)) {
            return Double.parseDouble(limit);
        } else {
            return 0.0;
        }
    }

    public String getFileUrlField() {
        return getConfig(FILE_URL_FIELD, DEFAULT_CONFIG_FILE);
    }

    public Map<String, String> getUrlSubstitution(String layer) {
        try {
            return getConfigMap(URL_SUBSTITUTION, getLayerConfigFilePath(layer));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Could not read url substitutions for %s", layer), e);
        }
    }

    public Set<String> getVariablesToInclude(String layer)  {
        try {
            return new LinkedHashSet<>(getConfigList(VARIABLES_TO_INCLUDE, getLayerConfigFilePath(layer)));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading variables to include from config for %s", layer), e);
        }
    }

    public String getTitle(String layer) {
        try {
            return getConfig(TITLE, getLayerConfigFilePath(layer));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading title attribute from config for %s", layer), e);
        }
    }

    public String getLatitudeStart(String layer) {
        try {
            return getConfig(LATITUDE_START, getLayerConfigFilePath(layer));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading latitude start attribute from config for %s", layer), e);
        }
    }

    public String getLatitudeEnd(String layer) {
        try {
            return getConfig(LATITUDE_END, getLayerConfigFilePath(layer));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading latitude end attribute from config for %s", layer), e);
        }
    }

    public String getLongitudeStart(String layer) {
        try {
            return getConfig(LONGITUDE_START, getLayerConfigFilePath(layer));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading longitude start attribute from config for %s", layer), e);
        }
    }

    public String getLongitudeEnd(String layer) {
        try {
            return getConfig(LONGITUDE_END, getLayerConfigFilePath(layer));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading longitude end attribute from config for %s", layer), e);
        }
    }

    public List<String> getTimeStart(String layer) {
        try {
            return getConfigList(TIME_START, getLayerConfigFilePath(layer));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading time start attribute from config for %s", layer), e);
        }
    }

    public List<String> getTimeEnd(String layer) {
        try {
            return getConfigList(TIME_END, getLayerConfigFilePath(layer));
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading time end attribute from config for %s", layer), e);
        }
    }

    public Map<String, ValueTemplate> getTemplatedAttributes(String featureTypeName) {
        Map<String, ValueTemplate> result = new LinkedHashMap<>();

        // Support current method of specifying attributes to add or replace

        result.put(
            getTitle(featureTypeName),
            new ValueTemplate(Pattern.compile("(.*?)(,[^,]*)?"), "${1}, ${TIME_START}, ${TIME_END}")
        );

        result.put(getLatitudeStart(featureTypeName), new ValueTemplate("${LAT_MIN}"));
        result.put(getLatitudeEnd(featureTypeName), new ValueTemplate("${LAT_MAX}"));

        result.put(getLongitudeStart(featureTypeName), new ValueTemplate("${LON_MIN}"));
        result.put(getLongitudeEnd(featureTypeName), new ValueTemplate("${LON_MAX}"));

        for (String timeStartEntry : getTimeStart(featureTypeName)) {
            result.put(timeStartEntry, new ValueTemplate("${TIME_START}"));
        }

        for (String timeEndEntry : getTimeEnd(featureTypeName)) {
            result.put(timeEndEntry, new ValueTemplate("${TIME_END}"));
        }

        //TODO: Support more generic way of specifying changes to attributes

        return result;
    }


    private String getLayerConfigFilePath(String layer) throws Exception {
        return getLayerConfigPath(layer, GOGODUCK_CONFIG_FILE);
    }
}
