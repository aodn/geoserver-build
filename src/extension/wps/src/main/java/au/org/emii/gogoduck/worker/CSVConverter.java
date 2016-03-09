package au.org.emii.gogoduck.worker;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVConverter extends Converter {

    private String ncksPath = null;
    private String ncpdqPath = null;
    private final String mimeType = "text/csv";
    private final String extension = "csv";
    private Integer lineNumber = 0;
    private Integer attributesEnd = 0;
    private List<String> lines = new ArrayList<String>();
    private List<String> metadata = new ArrayList<String>();
    private List<String> attributeMetadata = new ArrayList<String>();
    private List<Map> attributes = new ArrayList<Map>();
    private StringBuilder csvString = new StringBuilder();

    private final String NEW_LINE = System.getProperty("line.separator");
    private final String COMMENT = "# ";
    private static final Logger logger = LoggerFactory.getLogger(CSVConverter.class);

    public CSVConverter() {}

    @Override
    public void init() {
        this.ncksPath = GoGoDuckConfig.ncksPath;
        this.ncpdqPath = GoGoDuckConfig.ncpdqPath;
    }

    @Override
    public String getMimeType() { return mimeType; }

    @Override
    public String getExtension() { return extension; }

    @Override
    public Path convert(Path outputFile) throws GoGoDuckException {
        String baseFilename = FilenameUtils.getBaseName(String.valueOf(outputFile));
        try {
            File unpackedNetCDF = unpackNetCDF(outputFile.toFile());
            Path csvPath = executeNcksAndOutputToFile(baseFilename, unpackedNetCDF.toPath());
            Files.delete(unpackedNetCDF.toPath());
            return csvPath;
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Could not convert output to csv: '%s'", e.getMessage()));
        }
    }

    private File unpackNetCDF(File netCDFInputFile) throws Exception {
        List<String> ncpdqCommand = new ArrayList<>();
        File unpackedNetCDF = File.createTempFile("tmp", ".nc");

        ncpdqCommand.add(ncpdqPath);
        ncpdqCommand.add("-U");
        ncpdqCommand.add("-O");
        ncpdqCommand.add(netCDFInputFile.toString());
        ncpdqCommand.add(unpackedNetCDF.getPath());

        GoGoDuck.execute(ncpdqCommand);
        return unpackedNetCDF;
    }

    private Path executeNcksAndOutputToFile(String baseFilename, Path tempFile) throws IOException, InterruptedException {
        List<String> ncksCommand = new ArrayList<>();
        ncksCommand.add(ncksPath);
        ncksCommand.add("--no_nm_prn");
        ncksCommand.add("-a");
        ncksCommand.add(tempFile.toString());

        logger.info(ncksCommand.toString());
        
        ProcessBuilder processor = new ProcessBuilder(ncksCommand);
        processor.redirectError(ProcessBuilder.Redirect.PIPE);

        final Process process = processor.start();
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        InputStreamReader errorStream = new InputStreamReader(process.getErrorStream());
        BufferedReader bufferedErrorStream = new BufferedReader(errorStream);
        
        StringBuilder builder = new StringBuilder();
        String line;
        String errLine = null;
        while ((line = br.readLine()) != null || (errLine = bufferedErrorStream.readLine()) != null) {
            if (line != null) {
                builder.append(line + System.getProperty("line.separator"));
            }
            if (errLine != null) {
                logger.error(errLine);
            }
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            logger.error(String.format("Interrupted: '%s'", e.getMessage()));
            throw e;
        }

        String rawOutput = builder.toString();
        String outputCSV = parseNcks(rawOutput);
        String csvFilename = String.format("%s.%s", baseFilename, extension);
        File csvFile = new File(csvFilename);
        FileUtils.writeStringToFile(csvFile, outputCSV);

        return csvFile.toPath();
    }

    private String parseNcks(String input) {
        InputStream is = new ByteArrayInputStream(input.getBytes());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line = br.readLine();

            while (line != null) {
                extractGlobalMetadata(line);
                extractAttributes(line);
                lines.add(line);

                line = br.readLine();
                lineNumber++;
            }

            for (Map attr : attributes) {
                extractDetailedAttributes(attr);
            }

            writeMetadata();
            writeDataBlocks();
        }
        catch (Exception e) {
            logger.error(e.toString());
        }

        return csvString.toString();
    }

    private void extractGlobalMetadata(String aText) {
        Pattern pattern = Pattern.compile("^Global", Pattern.COMMENTS);
        Matcher matcher = pattern.matcher(aText);

        if (matcher.find()) {
            metadata.add(String.format(COMMENT + "%s", aText.split(":", 2)[1].trim()));
        }
    }

    private void extractAttributes(String aText) {
        Pattern pattern = Pattern.compile("\\:\\stype", Pattern.COMMENTS);
        Matcher matcher = pattern.matcher(aText);

        while (matcher.find()) {
            Map<String, String> attr = new HashMap<String, String>();
            attr.put("name", aText.split(":", 2)[0].trim());
            attr.put("lineNumber", lineNumber.toString());

            attributes.add(attr);
        }
    }

    private void extractDetailedAttributes(Map attr) {
        String lNum = attr.get("lineNumber").toString();
        Integer lineNumber = Integer.parseInt(lNum);
        String line = lines.get(lineNumber);
        attributeMetadata.add(COMMENT);

        while (line.length() > 0) {
            setHeaders(attr,line);
            attributeMetadata.add(COMMENT + line);
            line = lines.get(lineNumber);
            lineNumber ++;
            setEndOfVariableBlock(lineNumber);
        }
    }

    private void setHeaders(Map attr, String line) {
        Pattern pattern = Pattern.compile("\\sdimension\\s[0-9]:", Pattern.COMMENTS);
        Matcher matcher = pattern.matcher(line);
        Integer dimensionCount = 1;

        while (matcher.find()) {
            String dimensionName = line.split(":", 2)[1].split(",", 2)[0].trim();

            if (attr.get("dimensions") == null) {
                attr.put("dimensions", dimensionName);
            } else {
                attr.put("dimensions", attr.get("dimensions") + "," + dimensionName);
            }

            attr.put("dimension_count", dimensionCount);
            dimensionCount ++;
        }
    }

    private void writeMetadata() {
        csvAppender("# METADATA:");

        for (String m : metadata) {
            csvAppender(m);
        }

        for (String a : attributeMetadata) {
            csvAppender(a);
        }
        csvAppender(NEW_LINE);
    }

    private void writeDataBlockHeaders(Map attr) {
        String header = attr.get("dimensions") + "," + attr.get("name");
        String dimensions = attr.get("dimensions").toString().replace(",", "");

        if (dimensions.equals(attr.get("name").toString())) {
            header = attr.get("name").toString();
        }

        csvAppender(header);
    }

    private void writeDataBlocks() {
        for (Map attr : attributes) {
            writeDataBlockHeaders(attr);
            String line = lines.get(attributesEnd );

            while (line.length() > 0) {
                line = lines.get(attributesEnd);
                attributesEnd++;

                csvAppender(line.replace(" ", ","));
            }
        }
    }

    private void setEndOfVariableBlock(Integer lineNumber) {
        if (lineNumber > attributesEnd) {
            attributesEnd = lineNumber;
        }
    }

    private void csvAppender(String line) {
        csvString.append(line + NEW_LINE);
    }
}
