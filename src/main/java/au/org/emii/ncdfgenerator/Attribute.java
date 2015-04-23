
package au.org.emii.ncdfgenerator;

class Attribute
{
	final String name;
	final String value;
	final String sql;

	public Attribute( String name, String value, String sql )
	{
		this.name = name;
		this.value = value;
		this.sql = sql;
	}
}

