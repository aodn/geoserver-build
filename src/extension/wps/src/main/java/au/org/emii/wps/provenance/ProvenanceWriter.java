package au.org.emii.wps.provenance;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.geoserver.platform.GeoServerResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class ProvenanceWriter {

    private static final Logger logger = LoggerFactory.getLogger(ProvenanceWriter.class);
    private static final String PROVENANCE_TEMPLATES = "wps/provenance";

    private final Configuration config;

    public ProvenanceWriter(GeoServerResourceLoader resourceLoader) {
        File templateDir = resourceLoader.get(PROVENANCE_TEMPLATES).dir();

        // Handle missing template directory
        if (templateDir == null) {
            logger.error("'" + PROVENANCE_TEMPLATES + "' directory not found");
            config = null;
            return;
        }

        config = new Configuration();
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        try {
            config.setDirectoryForTemplateLoading(templateDir);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not set templateDirectory", e);
        }
    }

    public ProvenanceWriter(Configuration config) {
        this.config = config;
    }

    public String write(String templatePath, Map<String, Object> parameters) {
        try {
            if (config == null) {
                return "Provenance templates not configured";
            }

            Template template = config.getTemplate(templatePath);
            StringWriter writer = new StringWriter();
            template.process(parameters, writer);
            return writer.toString();
        } catch (FileNotFoundException e) {
            logger.error("No template {} found for provenance document", templatePath);
            return "Provenance template '" + templatePath + "' not found";
        } catch (TemplateException|IOException e) {
            logger.error("Error loading provenance document", templatePath);
            return "Error loading provenance template '" + templatePath + "' not found";
        }
    }

}
