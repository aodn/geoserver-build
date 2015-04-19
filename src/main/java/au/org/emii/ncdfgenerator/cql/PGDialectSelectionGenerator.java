

package au.org.emii.ncdfgenerator.cql;

import java.lang.StringBuilder; 
import java.text.SimpleDateFormat;


public class PGDialectSelectionGenerator implements IExprVisitor
{
	StringBuilder b;
	SimpleDateFormat df;

	public PGDialectSelectionGenerator( StringBuilder b )
	{
		this.b = b;
		this.df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	// think our naming is incorrect
	public void visit( ExprIntegerLiteral expr )
	{
		// This should actually emit a '?' and load the value into the sql parameter list
		// to avoid sql injection
		b.append( expr.value );
	}

	public void visit( ExprTimestampLiteral expr )
	{
		b.append( "'" + df.format(expr.value ) + "'" );
	}

	public void visit( ExprStringLiteral expr )
	{
		b.append("'"+ expr.value + "'");
	}

	public void visit( ExprSymbol expr )
	{
		// must quote field to enforce full case handling
		b.append('\"' + expr.value + '\"' );
	}

	public void visit(  ExprWKTLiteral expr ) throws Exception
	{
		// write( "WKTLiteral:" + expr.value );
	}


	public void visit( ExprProc expr ) throws Exception
	{
		String symbol = expr.symbol;

		if(symbol.equals("equals")) {
			emitInfixSqlExpr( "=", expr );
		}
		else if(symbol.equals("gt")) {
			emitInfixSqlExpr( ">", expr );
		}
		else if(symbol.equals("lt")) {
			emitInfixSqlExpr( "<", expr );
		}
		else if(symbol.equals("geq")) {
			emitInfixSqlExpr( ">=", expr );
		}
		else if( symbol.equals("and")
			|| symbol.equals("or")
			) {
			emitInfixSqlExpr( symbol, expr );
		}
		else {
			throw new CQLException( "Unrecognized proc expression symbol '" + symbol + "'" );
		}
	}

	public void emitInfixSqlExpr( String op, ExprProc expr ) throws Exception
	{
		// if expansion is done in order we may be ok,....
		b.append('(');
		expr.children.get(0).accept(this);
		b.append(' ');
		b.append(op);
		b.append(' ');
		expr.children.get(1).accept(this);
		b.append(')');
	}
}


