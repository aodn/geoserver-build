

package au.org.emii.ncdfgenerator.cql;


import java.lang.StringBuilder; 
import java.text.SimpleDateFormat;

import au.org.emii.ncdfgenerator.cql.CQLException;


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
		b.append( "ST_GeomFromText( '" + expr.value + "', 4326)" );
	}


	public void visit( ExprProc expr ) throws Exception
	{
		String symbol = expr.symbol;
		String lower = symbol.toLowerCase(); 

		if( symbol.equals("nop")) {
			if( expr.children.size() != 1) {
				// should almost be an unchecked runtime exception
				throw new CQLException( "nop with more than one child" );
			}
			expr.children.get(0).accept(this);
		}
		else if( lower.equals("and")
			|| lower.equals("or")
			) {
			emitInfixSqlExpr( symbol, expr );
		}

		else if( lower.equals("intersects")) {
			emitFunctionSqlExpr ( "ST_INTERSECTS", expr );
		}

		else if(symbol.equals(">=")
			|| symbol.equals("<=")
			|| symbol.equals("<")
			|| symbol.equals(">")
		) {
			emitInfixSqlExpr( symbol, expr );
		}

		else {
			throw new CQLException( "Unrecognized proc expression symbol '" + symbol + "'" );
		}
	}

	public void emitFunctionSqlExpr( String op, ExprProc expr ) throws Exception
	{
		// if expansion is done in order we may be ok,....

		b.append(op);
		b.append('(');

		boolean first = true;
		for( IExpression child : expr.children ) {
			if( first ) {
				child.accept(this);
				first = false;
			}
			else {
				b.append(',');
				child.accept(this);
			}
		}
		b.append(')');
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


