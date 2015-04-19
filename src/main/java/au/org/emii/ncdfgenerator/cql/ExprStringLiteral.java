
package au.org.emii.ncdfgenerator.cql;

public class ExprStringLiteral implements IExpression
{
	final int pos;
	final String value;

	public ExprStringLiteral( int pos, String value)
	{
		this.pos = pos;
		this.value = value;
	}

	public int getPosition() { return pos; }
	public void accept( IExprVisitor v ) throws Exception
	{ v.visit( this); }
}



