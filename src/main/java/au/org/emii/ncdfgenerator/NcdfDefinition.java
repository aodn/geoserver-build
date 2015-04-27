
package au.org.emii.ncdfgenerator;

import java.util.List;

class NcdfDefinition {
    private final DataSource dataSource;
    private final List< Attribute> globalAttributes;
    private final List< IDimension> dimensions;
    private final List< IVariable> variables;

    NcdfDefinition(
        DataSource dataSource,
        List< Attribute> globalAttributes,
        List< IDimension> dimensions,
        List< IVariable> variables
    ) {
        this.dataSource = dataSource;
        this.globalAttributes = globalAttributes;
        this.dimensions = dimensions;
        this.variables = variables;
    }

    DataSource getDataSource() {
        return dataSource;
    }

    List<IDimension> getDimensions() {
        return dimensions;
    }

    List<IVariable> getVariables() {
        return variables;
    }

    List<Attribute> getGlobalAttributes() {
        return globalAttributes;
    }
}

