package au.org.emii.wps.gogoduck.config;


import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.xstream.AggregationOverridesReader;
import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import au.org.emii.aggregator.overrides.GlobalAttributeOverrides;
import au.org.emii.aggregator.overrides.VariableOverrides;
import au.org.emii.core.Config;
import au.org.emii.wps.gogoduck.exception.GoGoDuckException;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class GoGoDuckConfig extends Config {

    private static final String GOGODUCK_CONFIG_FILE = "gogoduck.xml";
    private static final String DEFAULT_CONFIG_FILE = "wps/gogoduck.xml";
    private static final String FILE_PREFIX = "/gogoduck/";
    private static final String GLOBAL_ATTRIBUTE_PREFIX = FILE_PREFIX + "globalAttributes/";
    private static final String VARIABLES_TO_INCLUDE = FILE_PREFIX + "variablesToInclude/variable";
    private static final String TEMPLATES = FILE_PREFIX + "templates/template";

    private static final String FILE_LIMIT_KEY = FILE_PREFIX + "fileLimit";
    private static final String URL_SUBSTITUTION = FILE_PREFIX + "urlSubstitution";

    private static final String TITLE = GLOBAL_ATTRIBUTE_PREFIX + "title";
    private static final String LATITUDE_START = GLOBAL_ATTRIBUTE_PREFIX + "latitudeStart";
    private static final String LATITUDE_END = GLOBAL_ATTRIBUTE_PREFIX + "latitudeEnd";
    private static final String LONGITUDE_START = GLOBAL_ATTRIBUTE_PREFIX + "longitudeStart";
    private static final String LONGITUDE_END = GLOBAL_ATTRIBUTE_PREFIX + "longitudeEnd";
    private static final String TIME_START = GLOBAL_ATTRIBUTE_PREFIX + "timeStart";
    private static final String TIME_END = GLOBAL_ATTRIBUTE_PREFIX + "timeEnd";

    private static final String TIME_FIELD = FILE_PREFIX + "timeField";
    private static final String SIZE_FIELD = FILE_PREFIX + "sizeField";
    private static final String FILE_SIZE_LIMIT = FILE_PREFIX + "fileSizeLimit";
    private static final String FILE_URL_FIELD = FILE_PREFIX + "fileUrlField";

    private static final String STORAGE_LIMIT_KEY = "storageLimit";
    private static final String CONNECT_TIMEOUT_KEY = "connectTimeOut";
    private static final String READ_TIMEOUT_KEY = "readTimeOut";
    private static final String THREAD_COUNT_KEY = "poolSize";
    private static final String MAX_CHUNK_SIZE_KEY = "maxChunkSize";

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

    public List<VariableOverrides> getVariables(String layer)  {
        try {
            List<VariableOverrides> result = new ArrayList<>();

            for (String variableName: getConfigList(VARIABLES_TO_INCLUDE, getLayerConfigFilePath(layer))) {
                result.add(new VariableOverrides(variableName));
            }

            return result;
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

    public List<GlobalAttributeOverride> getGlobalAttributes(String featureTypeName) {
        List<GlobalAttributeOverride> result = new ArrayList<>();

        // Support current method of specifying attributes to add or replace

        result.add(
            new GlobalAttributeOverride(getTitle(featureTypeName), "(.*?)(,[^,]*)?",
                "${1}, ${TIME_START}, ${TIME_END}")
        );

        result.add(new GlobalAttributeOverride(getLatitudeStart(featureTypeName), "${LAT_MIN}"));
        result.add(new GlobalAttributeOverride(getLatitudeEnd(featureTypeName), "${LAT_MAX}"));

        result.add(new GlobalAttributeOverride(getLongitudeStart(featureTypeName), "${LON_MIN}"));
        result.add(new GlobalAttributeOverride(getLongitudeEnd(featureTypeName), "${LON_MAX}"));

        for (String timeStartEntry : getTimeStart(featureTypeName)) {
            result.add(new GlobalAttributeOverride(timeStartEntry, "${TIME_START}"));
        }

        for (String timeEndEntry : getTimeEnd(featureTypeName)) {
            result.add(new GlobalAttributeOverride(timeEndEntry, "${TIME_END}"));
        }

        return result;
    }

    public AggregationOverrides getTemplate(String layer) {
        try {
            AggregationOverrides aggregationOverrides = loadTemplate(layer);

            // For the moment default to creating template from old config until replaced
            if (aggregationOverrides.isEmpty()) {
                GlobalAttributeOverrides attributeOverrides = new GlobalAttributeOverrides(new ArrayList<String>(), getGlobalAttributes(layer));
                return new AggregationOverrides(attributeOverrides, getVariables(layer));
            }

            return aggregationOverrides;
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Error reading aggregation overrides for layer %s", layer), e);
        }

    }

    public AggregationOverrides loadTemplate(String layer) throws Exception {
        String templatePath = getTemplatePath(layer);

        if (templatePath == null) {
            // Default overrides (none)
            return new AggregationOverrides();
        }

        return AggregationOverridesReader.load(Paths.get(getConfigFilePath(templatePath)));
    }

    public String getTemplatePath(String layer) throws Exception {
        Map<String, String> templates = getConfigMap(TEMPLATES, "match", getLayerConfigFilePath(layer));

        String templateName = null;

        for (Entry<String, String> entry: templates.entrySet()) {
            if (Pattern.matches(entry.getKey(), layer)) {
                templateName = entry.getValue();
                break;
            }
        }

        return templateName == null ? null : "wps/" + templateName + ".xml";
    }

    public String getLayerConfigFilePath(String layer) throws Exception {
        return getLayerConfigPath(layer, GOGODUCK_CONFIG_FILE);
    }

    public long getStorageLimit() {
        String storageLimit = getConfig(STORAGE_LIMIT_KEY, DEFAULT_CONFIG_FILE);
        
        if (storageLimit == null) {
            return 200 * 1024 * 1024; // default is 200 MiB
        } else {
            return Long.parseLong(storageLimit);
        }
    }

    public int getConnectTimeOut() {
        String connectTimeOut = getConfig(CONNECT_TIMEOUT_KEY, DEFAULT_CONFIG_FILE);

        if (connectTimeOut == null) {
            return 60 * 1000; // default is 60 seconds
        } else {
            return Integer.parseInt(connectTimeOut);
        }
    }

    public int getReadTimeOut() {
        String readTimeOut = getConfig(READ_TIMEOUT_KEY, DEFAULT_CONFIG_FILE);

        if (readTimeOut == null) {
            return 60 * 1000; // default is 60 seconds
        } else {
            return Integer.parseInt(readTimeOut);
        }
    }

    public int getThreadCount() {
        String threadCount = getConfig(THREAD_COUNT_KEY, DEFAULT_CONFIG_FILE);

        if (threadCount == null) {
            return 8; // default is 8 threads
        } else {
            return Integer.parseInt(threadCount);
        }
    }

    public Long getMaxChunkSize() {
        String maxChunkSize = getConfig(MAX_CHUNK_SIZE_KEY, DEFAULT_CONFIG_FILE);

        if (maxChunkSize == null) {
            return null;
        } else {
            return new Long(maxChunkSize);
        }
    }
}
