package au.org.emii.aggregator.template;

import au.org.emii.aggregator.dataset.AbstractNetcdfDataset;
import au.org.emii.aggregator.dataset.NetcdfDatasetIF;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Template output dataset definition - contains all non-time varying information to be
 * written to output dataset.
 */
public class TemplateDataset extends AbstractNetcdfDataset {
    private final List<Attribute> globalAttributes;
    private final List<Dimension> dimensions;
    private final List<NetcdfVariable> variables;

    public TemplateDataset(NetcdfDatasetIF dataset, Set<String> requestedVariables,
                           Map<String, ValueTemplate> attributeModifications,
                           CalendarDateRange timeRange, Range verticalSubset, LatLonRect bbox) {

        this.globalAttributes = getGlobalAttributes(dataset, attributeModifications, timeRange, verticalSubset, bbox);

        // Determine variables/dimensions to add

        List<NetcdfVariable> templateVariables = new ArrayList<>();
        Map<String, Dimension> templateDimensions = new LinkedHashMap<>();

        String timeDimension = dataset.getTimeAxis().getDimensionName();

        for (NetcdfVariable variable: dataset.getVariables()) {
            if (requestedVariables != null && !requestedVariables.contains(variable.getShortName())) {
                continue;
            }

            TemplateVariable templateVariable = new TemplateVariable(variable, timeDimension);

            templateVariables.add(templateVariable);

            for (Dimension templateDimension: templateVariable.getDimensions()) {
                templateDimensions.put(templateDimension.getShortName(), templateDimension);
            }
        }

        // Get template dimensions in original dataset order

        List<Dimension> orderedTemplateDimensions = new ArrayList<>();

        for (Dimension dimension: dataset.getDimensions()) {
            Dimension templateDimension = templateDimensions.get(dimension.getShortName());

            if (templateDimension != null) {
                orderedTemplateDimensions.add(templateDimension);
            }
        }

        this.dimensions = new ArrayList<>(orderedTemplateDimensions);
        this.variables = new ArrayList<>(templateVariables);
    }

    @Override
    public List<Attribute> getGlobalAttributes() {
        return globalAttributes;
    }

    @Override
    public List<NetcdfVariable> getVariables() {
        return variables;
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    private List<Attribute> getGlobalAttributes(NetcdfDatasetIF dataset, Map<String, ValueTemplate> attributeModifications, CalendarDateRange timeRange, Range verticalSubset, LatLonRect bbox) {
        // Build substitutable values

        LatLonRect newBbox = dataset.getBbox();

        Map<String, String> substitutableValues = new LinkedHashMap<>();

        substitutableValues.put("LAT_MIN", Double.toString(newBbox.getLatMin()));
        substitutableValues.put("LAT_MAX", Double.toString(newBbox.getLatMax()));
        substitutableValues.put("LON_MIN", Double.toString(newBbox.getLonMin()));
        substitutableValues.put("LON_MAX", Double.toString(newBbox.getLonMax()));

        if (timeRange != null) {
            substitutableValues.put("TIME_START", timeRange.getStart().toString());
            substitutableValues.put("TIME_END", timeRange.getStart().toString());
        }

        // Build modified attribute list

        Map<String, Attribute> result = new LinkedHashMap<>();

        for (Attribute attribute : dataset.getGlobalAttributes()) {
            result.put(attribute.getShortName(), attribute);
        }

        // Make requested modifications

        for (Entry<String, ValueTemplate> modification : attributeModifications.entrySet()) {
            String attributeName = modification.getKey();
            ValueTemplate valueTemplate = modification.getValue();

            if (result.containsKey(attributeName)) {
                // modify existing attribute
                Attribute currentAttribute = result.get(attributeName);

                if (currentAttribute.isArray()) {
                    throw new UnsupportedOperationException("Update of array attributes not supported");
                }

                String currentValue = currentAttribute.isString() ? currentAttribute.getStringValue() : currentAttribute.getNumericValue().toString();
                String newValue = valueTemplate.getValue(currentValue, substitutableValues);
                final Attribute modifiedAttribute = new Attribute(currentAttribute.getShortName(), valueTemplate.getValue(currentValue, substitutableValues));
                result.put(modifiedAttribute.getShortName(), modifiedAttribute);
            } else {
                // add new attribute
                result.put(attributeName, new Attribute(attributeName, valueTemplate.getValue(substitutableValues)));
            }
        }

        return new ArrayList<>(result.values());
    }

}
