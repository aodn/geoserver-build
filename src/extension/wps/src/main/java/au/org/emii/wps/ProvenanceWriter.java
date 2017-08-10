package au.org.emii.wps;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;


public class ProvenanceWriter {

    private static final String TEMPLATE_NAME = "provenance_template_gridded.ftl";
    Configuration config = new Configuration();


    public ProvenanceWriter() {
        config.setClassForTemplateLoading(ProvenanceWriter.class, "");
    }

    public void write(Writer writer) throws TemplateException, IOException {

        Template template = config.getTemplate(TEMPLATE_NAME);
        Map<String, Object> root = new HashMap<String, Object>(); // todo populate
        root.put("test", "Hi Craig testing out freemarker");
        template.process(root, writer);
    }

    public String toString() {
        try {
            Template template = config.getTemplate(TEMPLATE_NAME);
            return template.toString();
        }
        catch(Exception e){
            return "Couldnt find it";
        }
    }


}
