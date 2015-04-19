
package au.org.emii.ncdfgenerator.cql;

public interface IExprVisitor
{
	public void visit( ExprIntegerLiteral expr ) throws Exception;
	public void visit( ExprStringLiteral expr ) throws Exception;
	public void visit( ExprTimestampLiteral expr ) throws Exception;
	public void visit( ExprWKTLiteral expr ) throws Exception;
	public void visit( ExprSymbol expr ) throws Exception;
	public void visit( ExprProc expr ) throws Exception;
}


