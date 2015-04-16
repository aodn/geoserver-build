
package au.org.emii.ncdfgenerator.cql;

import java.sql.Timestamp ;

public class ExprTimestampLiteral implements IExpression
{
	final int pos;
	final Timestamp value;

	public ExprTimestampLiteral( int pos, Timestamp value)
	{
		this.pos = pos;
		this.value = value;
	}

	public int getPosition() { return pos; }
	public void accept( IExprVisitor v ) throws Exception
	{ v.visit( this); }
}
