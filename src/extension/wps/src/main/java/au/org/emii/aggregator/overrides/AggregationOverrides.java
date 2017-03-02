package au.org.emii.aggregator.overrides;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration applicable to aggregation
 */

@XStreamAlias("template")
@XStreamConverter(AggregationOverridesConverter.class)
public class AggregationOverrides {
    private List<GlobalAttributeOverride> attributes;
    private List<VariableOverrides> variableOverridesList;

    public AggregationOverrides(List<GlobalAttributeOverride> attributes, List<VariableOverrides> variableOverridesList) {
        this.attributes = attributes;
        this.variableOverridesList = variableOverridesList;
    }

    public AggregationOverrides() {
        this.attributes = new ArrayList<>();
        this.variableOverridesList = new ArrayList<>();
    }

    public List<GlobalAttributeOverride> getAttributes() {
        return attributes;
    }

    public List<VariableOverrides> getVariableOverridesList() {
        return variableOverridesList;
    }

    public boolean includeVariable(String name) {
        return variableOverridesList.size() == 0 // no variable overrides specified means include all variables
            || hasVariableOverride(name); // variable overrides specified and the variable is included
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("attributes=");

        for (GlobalAttributeOverride attribute: attributes) {
            builder.append(attribute.getName());
            builder.append(" ");
        }

        builder.append("variables=");

        for (VariableOverrides variableOverrides : this.variableOverridesList) {
            builder.append(variableOverrides.getName());
            builder.append(" ");
        }

        return builder.toString();
    }

    private boolean hasVariableOverride(String name) {
        return findVariableOverride(name) != null;
    }

    private VariableOverrides findVariableOverride(String name) {
        for (VariableOverrides overrides: variableOverridesList) {
            if (overrides.getName().equals(name)) {
                return overrides;
            }
        }

        return null;
    }
}
