package au.org.emii.geoserver.wfs.response.config;

import java.util.ArrayList;
import java.util.List;

public class PivotConfig {

    private String orderDirection;
    private String defaultValue;
    private List<String> excludedFields;

    public PivotConfig() {
        this.excludedFields = new ArrayList<>();
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(String orderDirection) {
        this.orderDirection = orderDirection;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<String> getExcludedFields() {
        return excludedFields;
    }

    public void setExcludedFields(List<String> excludedFields) {
        this.excludedFields = excludedFields;
    }

    public void addExcludedField(String fieldName) {
        this.excludedFields.add(fieldName);
    }
}
