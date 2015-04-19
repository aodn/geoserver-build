
package au.org.emii.ncdfgenerator.cql;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.io.IOException;



public class PrettyPrinterVisitor implements IExprVisitor
{

	final OutputStream os;

	public PrettyPrinterVisitor( OutputStream os)
	{
		this.os = os;
	}

	private void write( String s )
		throws IOException
	{
		os.write( s.getBytes(Charset.forName("UTF-8")));
	}

	public void visit( ExprSymbol expr ) throws Exception
	{
		write( "Symbol:" + expr.value );
	}

	public void visit(  ExprIntegerLiteral expr ) throws Exception
	{
		write( "Integer:" + expr.value );
	}

	public void visit( ExprTimestampLiteral expr ) throws Exception
	{
		write( "Timestamp:" + expr.value );
	}

	public void visit(  ExprStringLiteral expr ) throws Exception
	{
		write( "Literal:" + expr.value );
	}

	public void visit(  ExprWKTLiteral expr ) throws Exception
	{
		write( "WKTLiteral:" + expr.value );
	}

	public void visit( ExprProc expr ) throws Exception
	{
		write( "(" + expr.symbol + " " );
		for( IExpression child : expr.children ) {
			child.accept(this);
			write(  " ");
		}
		write( ")" );
	}
}

