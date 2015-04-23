
package au.org.emii.ncdfgenerator;

class DataSource
{
	final String schema;
	final String virtualDataTable;
	final String virtualInstanceTable;

	public DataSource( String schema, String virtualDataTable, String virtualInstanceTable )
	{
		this.schema = schema;
		this.virtualDataTable = virtualDataTable;
		this.virtualInstanceTable = virtualInstanceTable;
	}
}

