package au.org.emii.wps.provenance;

import java.util.Map;

public interface ProvenanceWriter {
    String write(String templatePath, Map<String, Object> parameters);
}
