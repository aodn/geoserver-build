package au.org.emii.aggregator;

import au.org.emii.aggregator.dataset.NetcdfDatasetAdapter;
import au.org.emii.aggregator.dataset.NetcdfDatasetIF;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.template.ValueTemplate;
import au.org.emii.aggregator.template.TemplateDataset;
import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.aggregator.variable.UnpackerOverrides;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImmutable;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * NetCDF Aggregator
 */

public class NetcdfAggregator implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(NetcdfAggregator.class);
    private static final Group GLOBAL = null;

    private final Path outputPath;
    private final Set<String> requestedVariables;
    private final LatLonRect bbox;
    private final Range verticalSubset;
    private final CalendarDateRange dateRange;
    private final Map<String, ValueTemplate> attributeChanges;
    private final Map<String, UnpackerOverrides> configuredOverrides;

    private Map<String, UnpackerOverrides> unpackerOverrides;

    private NetcdfFileWriter writer;

    private NetcdfDatasetIF templateDataset;

    public NetcdfAggregator(Path outputPath, Set<String> requestedVariables,
                            LatLonRect bbox, Range verticalSubset, CalendarDateRange dateRange,
                            Map<String, ValueTemplate> attributeChanges,
                            Map<String, UnpackerOverrides> configuredOverrides
    ) {
        this.outputPath = outputPath;
        this.requestedVariables = requestedVariables;
        this.bbox = bbox;
        this.verticalSubset = verticalSubset;
        this.dateRange = dateRange;
        this.attributeChanges = attributeChanges != null ? attributeChanges : new HashMap<String, ValueTemplate>();
        this.configuredOverrides = configuredOverrides != null ? configuredOverrides : new HashMap<String, UnpackerOverrides>();
    }

    public void add(Path datasetLocation) throws AggregationException {
        try (NetcdfDatasetAdapter dataset = NetcdfDatasetAdapter.open(datasetLocation, getUnpackerOverrides())) {
            NetcdfDatasetIF subsettedDataset = dataset.subset(dateRange, verticalSubset, bbox);

            if (!Files.exists(outputPath)) {
                logger.info("Creating output file {} using {} as a template", outputPath, datasetLocation);
                templateDataset = new TemplateDataset(subsettedDataset, requestedVariables, attributeChanges,
                    dateRange, verticalSubset, bbox);
                createFileFromTemplate(templateDataset);
                unpackerOverrides = dataset.getUnpackerOverrides();
            }

            logger.info("Adding {} to output file", datasetLocation);

            appendRecordVariables(subsettedDataset);
        } catch (IOException e) {
            throw new AggregationException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    private void createFileFromTemplate(NetcdfDatasetIF template) throws AggregationException {
        try {
            writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4,
                outputPath.toString(), null);

            // Copy global attributes to output file

            for (Attribute attribute : template.getGlobalAttributes()) {
                writer.addGroupAttribute(GLOBAL, attribute);
            }

            // Copy dimensions to output file

            List<Dimension> fileDimensions = new ArrayList<Dimension>();

            for (Dimension dimension: template.getDimensions()) {
                Dimension fileDimension = writer.addDimension(GLOBAL, dimension.getShortName(), dimension.getLength(), true, dimension.isUnlimited(), dimension.isVariableLength());
                fileDimensions.add(fileDimension);
            }

            // Copy variables to output file

            for (NetcdfVariable variable: template.getVariables()) {
                List<Dimension> variableDimensions = new ArrayList<Dimension>();

                for (Dimension dimension: variable.getDimensions()) {
                    for (Dimension fileDimension: fileDimensions) {
                        if (fileDimension.getFullName().equals(dimension.getFullName())) {
                            variableDimensions.add(fileDimension);
                        }
                    }
                }

                Variable fileVariable = writer.addVariable(GLOBAL, variable.getShortName(), variable.getDataType(), variableDimensions);

                for (Attribute attribute: variable.getAttributes()) {
                    writer.addVariableAttribute(fileVariable, attribute);
                }
            }

            // Finished defining file contents

            writer.create();

            // Copy static data (coordinate axes, etc) to output file

            for (NetcdfVariable variable: template.getVariables()) {
                if (!variable.isUnlimited()) {
                    Variable fileVariable = writer.findVariable(variable.getShortName());
                    writer.write(fileVariable, variable.read());
                }
            }
        } catch (IOException|InvalidRangeException e) {
            throw new AggregationException("Could not create output file", e);
        }
    }

    private void appendRecordVariables(NetcdfDatasetIF dataset) throws AggregationException {
        try {
            for (NetcdfVariable templateVariable: templateDataset.getVariables()) {
                if (!templateVariable.isUnlimited()) {
                    continue;
                }

                NetcdfVariable datasetVariable = dataset.findVariable(templateVariable.getShortName());
                append(datasetVariable);
            }

            writer.flush();
        } catch (IOException e) {
            throw new AggregationException(e);
        }
    }

    private void append(NetcdfVariable srcVariable) throws AggregationException {
        try {
            Variable destVariable = writer.findVariable(srcVariable.getShortName());
            int[] shape = srcVariable.getShape();
            int[] slice = srcVariable.getShape();
            slice[0] = 1;
            int[] srcOrigin = new int[shape.length];
            int[] destShape = destVariable.getShape();
            int[] destOrigin = new int[destShape.length];
            destOrigin[0] = destShape[0];

            for (int i = 0; i < shape[0]; i++) {
                srcOrigin[0] = i;
                Array data = srcVariable.read(srcOrigin, slice);
                writer.write(destVariable, destOrigin, data);
                destOrigin[0]++;
            }
        } catch (IOException |InvalidRangeException e) {
            throw new AggregationException(e);
        }
    }

    private Map<String, UnpackerOverrides> getUnpackerOverrides() {
        if (templateDataset == null) {
            return configuredOverrides;
        }

        Map<String, UnpackerOverrides> result = new LinkedHashMap<>();

        for (NetcdfVariable variable: templateDataset.getVariables()) {
            result.put(variable.getShortName(), getUnpackerOverrides(variable));
        }

        return result;
    }

    private UnpackerOverrides getUnpackerOverrides(NetcdfVariable variable) {
        UnpackerOverrides.Builder builder = new UnpackerOverrides.Builder()
            .newDataType(variable.getDataType());

        // ensure same filler values applied

        Attribute fillerAttribute = variable.findAttribute(CDM.FILL_VALUE);

        if (fillerAttribute != null) {
            builder.newFillerValue(fillerAttribute.getNumericValue());
        }

        // ensure same missing values applied

        Attribute missingValuesAttribute = variable.findAttribute(CDM.MISSING_VALUE);

        if (missingValuesAttribute != null) {
            Number[] missingValues = new Number[missingValuesAttribute.getLength()];
            for (int i=0; i< missingValues.length; i++) {
                missingValues[i] = missingValuesAttribute.getNumericValue(i);
            }
            builder.newMissingValues(missingValues);
        }

        return builder.build();
    }

    public static void main(String[] args) throws ParseException, AggregationException, IOException, InvalidRangeException {
        Options options = new Options();

        options.addOption("b", true, "restrict to bounding box specified by left lower/right upper coordinates e.g. -b 120,-32,130,-29");
        options.addOption("v", true, "restrict aggregation to specified variables only e.g. -v TEMP,PSAL");
        options.addOption("z", true, "restrict data to specified z index range e.g. -z 2,4");
        options.addOption("t", true, "restrict data to specified date/time range in ISO 8601 format e.g. -t 2017-01-12T21:58:02Z,2017-01-12T22:58:02Z");
        options.addOption("a", true, "add or replace attribute with value e.g. -a \"creator=J.A.Bloggs\"");

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse( options, args );

        List<String> inputFiles = Files.readAllLines(Paths.get(line.getArgs()[0]), Charset.forName("utf-8"));
        Path outputFile = Paths.get(line.getArgs()[1]);

        String varArg = line.getOptionValue("v");
        String bboxArg = line.getOptionValue("b");
        String zSubsetArg = line.getOptionValue("z");
        String timeArg = line.getOptionValue("t");
        String[] attributeTemplateArgs = line.getOptionValues("a");

        Set<String> requestedVariables = null;

        if (varArg != null) {
            requestedVariables = new LinkedHashSet<>(Arrays.asList(varArg.split(",")));
        }

        LatLonRect bbox = null;

        if (bboxArg != null) {
            String[] bboxCoords = bboxArg.split(",");
            double minLon = Double.parseDouble(bboxCoords[0]);
            double minLat = Double.parseDouble(bboxCoords[1]);
            double maxLon = Double.parseDouble(bboxCoords[2]);
            double maxLat = Double.parseDouble(bboxCoords[3]);
            LatLonPoint lowerLeft = new LatLonPointImmutable(minLat, minLon);
            LatLonPoint upperRight = new LatLonPointImmutable(maxLat, maxLon);
            bbox = new LatLonRect(lowerLeft, upperRight);
        }

        Range zSubset = null;

        if (zSubsetArg != null) {
            String[] zSubsetIndexes = zSubsetArg.split(",");
            int startIndex = Integer.parseInt(zSubsetIndexes[0]);
            int endIndex = Integer.parseInt(zSubsetIndexes[1]);
            zSubset = new Range(startIndex, endIndex);
        }

        CalendarDateRange timeRange = null;

        if (timeRange != null) {
            String[] timeRangeComponents = timeArg.split(",");
            CalendarDate startTime = CalendarDate.parseISOformat("Gregorian", timeRangeComponents[0]);
            CalendarDate endTime = CalendarDate.parseISOformat("Gregorian", timeRangeComponents[1]);
            timeRange = CalendarDateRange.of(startTime, endTime);
        }

        Map<String, ValueTemplate> templatedAttributes = new LinkedHashMap<>();

        if (attributeTemplateArgs != null && attributeTemplateArgs.length > 0) {
            for (String attributeTemplateArg: attributeTemplateArgs) {
                String[] parts = attributeTemplateArg.split("::");
                if (parts.length == 3) {
                    templatedAttributes.put(parts[0], new ValueTemplate(Pattern.compile(parts[1]), parts[2]));
                } else {
                    templatedAttributes.put(parts[0], new ValueTemplate(parts[1]));
                }
            }
        }

        try (
            NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, requestedVariables, bbox, zSubset, timeRange, templatedAttributes, null)
        ) {
            for (String file:inputFiles) {
                if (file.trim().length() == 0) continue;
                netcdfAggregator.add(Paths.get(file));
            }
        }
    }

}
