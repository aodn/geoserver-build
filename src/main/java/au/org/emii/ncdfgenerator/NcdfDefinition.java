
package au.org.emii.ncdfgenerator;

import java.util.List;

class NcdfDefinition
{
	final DataSource dataSource;
	final List< Attribute> globalAttributes;
	final List< IDimension> dimensions;
	final List< IVariableEncoder> variables;

	NcdfDefinition(
		DataSource dataSource,
		List< Attribute> globalAttributes,
		List< IDimension> dimensions,
		List< IVariableEncoder> variables
	) {
		this.dataSource = dataSource;
		this.globalAttributes = globalAttributes;
		this.dimensions = dimensions;
		this.variables = variables;
	}
}


