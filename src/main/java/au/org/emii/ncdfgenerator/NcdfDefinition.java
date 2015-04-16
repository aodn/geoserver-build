
package au.org.emii.ncdfgenerator;

import java.util.Map;

class NcdfDefinition
{
	final String schema;
	final String virtualDataTable;
	final String virtualInstanceTable;
	final Map< String, IDimension> dimensions;
	final Map< String, IVariableEncoder> encoders;

	NcdfDefinition(
		String schema,
		String virtualDataTable,
		String virtualInstanceTable,
		Map< String, IDimension> dimensions,
		Map< String, IVariableEncoder> encoders
	) {
		this.schema = schema;
		this.virtualDataTable = virtualDataTable;
		this.virtualInstanceTable = virtualInstanceTable;
		this.dimensions = dimensions;
		this.encoders = encoders;
	}
}


