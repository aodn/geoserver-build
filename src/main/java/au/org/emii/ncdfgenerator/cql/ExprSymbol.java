
package au.org.emii.ncdfgenerator.cql;

public class ExprSymbol implements IExpression
{
	final int pos;
	final String value;

	public ExprSymbol( int pos, String value)
	{
		this.pos = pos;
		this.value = value;
	}

	public int getPosition() { return pos; }
	public void accept( IExprVisitor v ) throws Exception
	{ v.visit( this); }
}
