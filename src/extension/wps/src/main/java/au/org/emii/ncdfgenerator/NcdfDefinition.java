package au.org.emii.ncdfgenerator;

import java.util.List;

public class NcdfDefinition {
    private final DataSource dataSource;
    private final FilenameTemplate filenameTemplate;
    private final List<Attribute> globalAttributes;
    private final List<IDimension> dimensions;
    private final List<IVariable> variables;

    NcdfDefinition(
        DataSource dataSource,
        FilenameTemplate filenameTemplate,
        List<Attribute> globalAttributes,
        List<IDimension> dimensions,
        List<IVariable> variables
    ) {
        this.dataSource = dataSource;
        this.filenameTemplate = filenameTemplate;
        this.globalAttributes = globalAttributes;
        this.dimensions = dimensions;
        this.variables = variables;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public FilenameTemplate getFilenameTemplate() {
        return filenameTemplate;
    }

    public List<IDimension> getDimensions() {
        return dimensions;
    }

    public List<IVariable> getVariables() {
        return variables;
    }

    public List<Attribute> getGlobalAttributes() {
        return globalAttributes;
    }
}
