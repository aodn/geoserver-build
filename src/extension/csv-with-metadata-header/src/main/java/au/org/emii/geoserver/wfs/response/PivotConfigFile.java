/*
 * Copyright 2021 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.wfs.response;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

public class PivotConfigFile {

    public static final String PIVOT_CONFIGURATION_FILE_NAME = "pivot.xml";

    protected String dataDirectoryPath;

    private File file;

    public PivotConfigFile(String dataDirectoryPath) {
        this.dataDirectoryPath = dataDirectoryPath;
    }

    public PivotConfig getConfig() {
        if (getFile().exists()) {
            try {
                return readConfigFile(getFile());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new PivotConfig();
    }

    private PivotConfig readConfigFile(File file) throws ParserConfigurationException, SAXException, IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)){
            return read(fileInputStream);
        }
    }

    private File getFile() {
        if (file == null) {
            file = new File(getFilePath());
        }
        return file;
    }

    private String getFilePath() {
        return String.format("%s/%s", dataDirectoryPath, PIVOT_CONFIGURATION_FILE_NAME);
    }

    public PivotConfig read(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        return new PivotConfigParser(getDocument(inputStream)).parse();
    }

    public Document getDocument(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(inputStream);
    }
}
