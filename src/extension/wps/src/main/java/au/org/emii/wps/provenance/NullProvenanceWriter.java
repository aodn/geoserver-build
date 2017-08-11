package au.org.emii.wps.provenance;

import java.util.Map;

public class NullProvenanceWriter implements ProvenanceWriter {
    @Override
    public String write(String templatePath, Map<String, Object> parameters) {
        return "";
    }
}
