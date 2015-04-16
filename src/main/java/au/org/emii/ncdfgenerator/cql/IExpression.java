
package au.org.emii.ncdfgenerator.cql;

public interface IExpression
{
	public int getPosition() ;
	public void accept( IExprVisitor v ) throws Exception ;
}

