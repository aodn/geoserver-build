
package au.org.emii.ncdfgenerator.cql;

import java.util.ArrayList;

public class ExprProc implements IExpression
{
	final int pos;
	// public for unit tests
	public final String symbol;
	public final ArrayList<IExpression> children;

	public ExprProc( int pos, String symbol, ArrayList< IExpression> children  )
	{
		this.pos = pos;
		this.symbol = symbol;
		this.children = children;
	}

	public int getPosition() { return pos; }
	public void accept( IExprVisitor v )
	throws Exception { v.visit( this); }
}
