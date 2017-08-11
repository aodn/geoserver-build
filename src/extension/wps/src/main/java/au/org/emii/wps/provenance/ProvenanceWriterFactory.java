package au.org.emii.wps.provenance;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.geoserver.platform.GeoServerResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.io.IOException;

public class ProvenanceWriterFactory {
    private static final Logger logger = LoggerFactory.getLogger(ProvenanceWriterFactory.class);
    private final Path templateDir;

    public ProvenanceWriterFactory(GeoServerResourceLoader resourceLoader) {
        templateDir = resourceLoader.getBaseDirectory().toPath().resolve("wps/provenance");
    }

    public ProvenanceWriter getWriter() {
        Configuration config = new Configuration();
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        try {
            config.setDirectoryForTemplateLoading(templateDir.toFile());
            return new ProvenanceWriterImpl(config);
        } catch (IOException e) {
            logger.warn("Could not set templateDirectory", e);
            return new NullProvenanceWriter();
        }
    }
}
