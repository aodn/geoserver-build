package au.org.emii.aggregator.template;

import au.org.emii.aggregator.dataset.AbstractNetcdfDataset;
import au.org.emii.aggregator.dataset.NetcdfDatasetIF;
import au.org.emii.aggregator.overrides.GlobalAttributeOverrides;
import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.aggregator.datatype.NumericTypes;
import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import org.apache.commons.lang.text.StrSubstitutor;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod.Field;
import ucar.unidata.geoloc.LatLonRect;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template output dataset definition - contains all non-time varying information to be
 * written to output dataset.
 */
public class TemplateDataset extends AbstractNetcdfDataset {
    private final List<Attribute> globalAttributes;
    private final List<Dimension> dimensions;
    private final List<NetcdfVariable> variables;

    public TemplateDataset(NetcdfDatasetIF dataset, AggregationOverrides aggregationOverrides,
                           CalendarDateRange timeRange, Range verticalSubset, LatLonRect bbox) {

        this.globalAttributes = getGlobalAttributes(dataset, aggregationOverrides.getAttributeOverrides(), timeRange);

        // Determine variables/dimensions to add

        List<NetcdfVariable> templateVariables = new ArrayList<>();
        Map<String, Dimension> templateDimensions = new LinkedHashMap<>();

        String timeDimension = dataset.getTimeAxis().getDimensionName();

        for (NetcdfVariable variable: dataset.getVariables()) {
            if (!aggregationOverrides.includeVariable(variable.getShortName())) {
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

    private List<Attribute> getGlobalAttributes(NetcdfDatasetIF dataset, GlobalAttributeOverrides attributeOverrides,
                                                CalendarDateRange timeRange) {
        // Copy existing attributes

        Map<String, Attribute> result = new LinkedHashMap<>();

        for (Attribute attribute : dataset.getGlobalAttributes()) {
            if (!attributeOverrides.getRemoveAttributes().contains(attribute.getShortName())) {
                result.put(attribute.getShortName(), attribute);
            }
        }

        // Apply attribute overrides

        Map<String, String> commonSubstitutionValues = getSubstitutionValues(dataset, timeRange);

        for (GlobalAttributeOverride override: attributeOverrides.getAddOrReplaceAttributes()) {
            String attributeName = override.getName();
            Pattern capturingPattern = override.getPattern();
            String templateValue = override.getValue();

            Map<String, String> attributeSubstitutionValues = new LinkedHashMap<>(commonSubstitutionValues);

            if (capturingPattern != null && result.containsKey(attributeName)) {
                Attribute attribute = result.get(attributeName);

                if (attribute.isArray()) {
                    throw new UnsupportedOperationException("Applying capturing patterns to array attributes not supported");
                }

                String currentValue = attribute.isString() ? attribute.getStringValue() :
                    attribute.getNumericValue().toString();

                Matcher matcher = capturingPattern.matcher(currentValue);

                if (matcher.matches()) {
                    for (int i = 0; i <= matcher.groupCount(); i++) {
                        attributeSubstitutionValues.put(Integer.toString(i), matcher.group(i));
                    }
                }

            }

            StrSubstitutor sub = new StrSubstitutor(attributeSubstitutionValues);
            String value = sub.replace(templateValue);

            result.put(attributeName, createNewAttribute(attributeName, override.getType(), value));
        }

        return new ArrayList<>(result.values());
    }

    private Map<String, String> getSubstitutionValues(NetcdfDatasetIF dataset, CalendarDateRange timeRange) {
        Map<String, String> result = new LinkedHashMap<>();

        LatLonRect newBbox = dataset.getBbox();

        result.put("LAT_MIN", Double.toString(newBbox.getLatMin()));
        result.put("LAT_MAX", Double.toString(newBbox.getLatMax()));
        result.put("LON_MIN", Double.toString(newBbox.getLonMin()));
        result.put("LON_MAX", Double.toString(newBbox.getLonMax()));

        if (timeRange != null) {
            result.put("TIME_START", timeRange.getStart().toString());
            result.put("TIME_END", timeRange.getEnd().toString());
        }

        CalendarDate aggregationTime = CalendarDate.of(new Date()).truncate(Field.Minute); // ignore seconds/milliseconds

        result.put("AGGREGATION_TIME", aggregationTime.toString());

        return result;
    }

    private Attribute createNewAttribute(String name, DataType type, String value) {
        if (type.isNumeric()) {
            return new Attribute(name, NumericTypes.parse(type, value));
        } else {
            return new Attribute(name, value);
        }
    }
}
