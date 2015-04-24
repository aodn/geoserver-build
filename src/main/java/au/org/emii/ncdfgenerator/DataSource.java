
package au.org.emii.ncdfgenerator;

class DataSource
{
	private final String schema;
	private final String virtualDataTable;
	private final String virtualInstanceTable;

	public DataSource( String schema, String virtualDataTable, String virtualInstanceTable )
	{
		this.schema = schema;
		this.virtualDataTable = virtualDataTable;
		this.virtualInstanceTable = virtualInstanceTable;
	}

	public String getSchema() {
		return schema;
	}

	public String getVirtualDataTable() {
		return virtualDataTable;
	}

	public String getVirtualInstanceTable() {
		return virtualInstanceTable;
	}
}

