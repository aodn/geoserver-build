
package au.org.emii.ncdfgenerator.cql;

public class ExprIntegerLiteral implements IExpression
{
	final int pos;
	final int value;

	public ExprIntegerLiteral( int pos, int value)
	{
		this.pos = pos;
		this.value = value;
	}

	public int getPosition() { return pos; }
	public void accept( IExprVisitor v ) throws Exception
	{ v.visit( this); }
}
