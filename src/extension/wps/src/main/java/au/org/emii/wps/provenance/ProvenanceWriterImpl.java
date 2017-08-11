package au.org.emii.wps.provenance;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class ProvenanceWriterImpl implements ProvenanceWriter {

    private static final Logger logger = LoggerFactory.getLogger(ProvenanceWriterImpl.class);

    private final Configuration config;

    public ProvenanceWriterImpl(Configuration config) {
        this.config = config;
    }

    @Override
    public String write(String templatePath, Map<String, Object> parameters) {
        try {
            Template template = config.getTemplate(templatePath);
            StringWriter writer = new StringWriter();
            template.process(parameters, writer);
            return writer.toString();
        } catch (FileNotFoundException e) {
            logger.warn("No template {} found for provenance document", templatePath);
            return ""; // Don't fail because of this
        } catch (TemplateException|IOException e) {
            logger.error("Error loading provenance document", templatePath);
            return ""; // Don't fail because of this.
        }
    }

}
