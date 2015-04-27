
package au.org.emii.ncdfgenerator.cql;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.sql.Timestamp;


public class ExprParser implements IExprParser
{
	
	final SimpleDateFormat df;

	public ExprParser() {

		df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	private IExpression createBinaryExpr(int pos, String symbol, IExpression lhs, IExpression rhs )
	{
		// helper
		ArrayList<IExpression> children = new ArrayList<IExpression>();
		children.add( lhs );
		children.add( rhs );
		return new ExprProc ( pos, symbol, children );
	}


	private IExpression createUnaryExpr(int pos, String symbol, IExpression lhs )
	{
		// helper
		ArrayList<IExpression> children = new ArrayList<IExpression>();
		children.add( lhs );
		return new ExprProc ( pos, symbol, children );
	}


	private int peekChar(String s, int pos)
	{
		if(pos < s.length() ) 
			return s.charAt( pos ); 
		return -1;
	}


	private String peekOperator(String s, int pos )
	{
		final String ops [] = { "AND", "OR", "<>", "<=", ">=", "<", ">", "=" } ; // make static 
		for( String op : ops )
		{
			if( pos + op.length() < s.length() 
				&& s.substring( pos, pos + op.length()).equals(op)) 
			{
				return s.substring( pos, pos + op.length()); 
			}	
		}
		return "";
	}


	private int skipWhite(String s, int pos)
	{
		while( Character.isSpaceChar(peekChar(s , pos) ))
			++pos;
		return pos; 
	}


	public IExpression parseExpression(String s) throws CQLException
	{
		int pos = 0;
		IExpression expr = parseExpression1( s, pos ); 
		if( expr != null) {
			// ensure we got to the end, with nothing trailing
			pos = expr.getPosition();
			pos = skipWhite(s, pos);
			if(pos == s.length())
				return expr;	
		}
		throw new CQLException( "failed to parse expression '" + s + "'" );
	}	


	private IExpression parseExpression1(String s, int pos)
	{
		return parseOrExpression( s, pos); 
	}	


	private IExpression parseOrExpression(String s, int pos)
	{
		IExpression lhs = parseAndExpression( s, pos); 
		if( lhs == null ) 
			return null; 
		pos = lhs.getPosition();
		while( true ) {			
			pos = skipWhite( s, pos); 
			String op = peekOperator( s, pos); 
			if( op.equals( "OR"))
				pos += op.length();
			else 
				return lhs;
			IExpression rhs = parseOrExpression( s, pos); 
			if( rhs == null ) 
				return lhs; 
			lhs = createBinaryExpr( rhs.getPosition(), op, lhs, rhs );
		}
	}

	
	private IExpression parseAndExpression(String s, int pos)
	{
		IExpression lhs = parseEqualityExpression( s, pos); 
		if( lhs == null ) 
			return null; 
		pos = lhs.getPosition();
		while( true ) {			
			pos = skipWhite( s, pos); 
			String op = peekOperator( s, pos); 
			if( op.equals( "AND"))
				pos += op.length();
			else 
				return lhs;
			IExpression rhs = parseAndExpression( s, pos); 
			if( rhs == null ) 
				return lhs; 
			lhs = createBinaryExpr( rhs.getPosition(), op, lhs, rhs );
		}
	}


	private IExpression parseEqualityExpression(String s, int pos)
	{
		IExpression lhs = parseComparisonExpression( s, pos); 
		if( lhs == null ) 
			return null; 
		pos = lhs.getPosition();
		while( true ) {			
			pos = skipWhite( s, pos); 
			String op = peekOperator( s, pos); 
			if( op.equals( "=")
				|| op.equals( "<>")
				)
				pos += op.length();
			else 
				return lhs;
			IExpression rhs = parseEqualityExpression( s, pos); 
			if( rhs == null ) 
				return lhs; 
			lhs = createBinaryExpr( rhs.getPosition(), op, lhs, rhs );
		}
	}

	private IExpression parseComparisonExpression(String s, int pos)
	{
		IExpression lhs = parseFactor( s, pos); 
		if( lhs == null ) 
			return null; 
		pos = lhs.getPosition();
		while( true ) {			
			pos = skipWhite( s, pos); 
			String op = peekOperator( s, pos); 
			if( op.equals( "<")
				|| op.equals( ">")
				|| op.equals( "<=")
				|| op.equals( ">=")) {
				pos += op.length();
			}
			else 
				return lhs;
			IExpression rhs = parseComparisonExpression( s, pos); 
			if( rhs == null ) 
				return lhs; 
			lhs = createBinaryExpr( rhs.getPosition(), op, lhs, rhs );
		}
	}

	// public for unit
	private IExpression parseFactor(String s, int pos)
	{
		pos = skipWhite(s, pos);

		// nested parenthesis expr
		if( peekChar( s, pos ) == '(' ) { 
			++pos;
			IExpression expr = parseExpression1(s, pos); 
			if( expr == null) 
				return null;
			pos = expr.getPosition();
			pos = skipWhite( s, pos); 
			if( peekChar( s, pos ) != ')' )  
				return null;
			++pos;
			// we have to create a dummy node, to record the new pos
			return createUnaryExpr( pos, "nop", expr );
		}	


		IExpression expr;

		expr = parseQuotedTimestamp(s, pos);
		if(expr != null) 
			return expr;

		expr = parseStringLiteral(s, pos);
		if(expr != null)
			return expr;

		expr = parseFloatLiteral(s, pos);
		if (expr != null)
			return expr;
	
		expr = parseIntegerLiteral(s, pos);
		if( expr != null)
			return expr;

		expr = parseWKT(s, pos);
		if( expr != null)
			return expr;

		expr = parseFunction(s, pos);
		if( expr != null)
			return expr; 

		expr = parseSymbol(s, pos);
		if( expr != null)
			return expr;

		return null;
	}




	//////////////////////////
/*
	private ExprProc parseProc(String s, int pos)
	{
		String symbol = null;

		if(s.charAt(pos) != '(')
			return null;
		++pos;

		// advance whitespace
		while(Character.isSpaceChar(s.charAt(pos))) {
			++pos;
		}

		// symbol
		if(Character.isLetter(s.charAt(pos)) || s.charAt(pos) == '_' ) {
			StringBuilder b = new StringBuilder();
			while(Character.isLetter(s.charAt(pos))
				|| Character.isDigit(s.charAt(pos))
				|| s.charAt(pos) == '_') {
				b.append(s.charAt(pos));
				++pos;
			}
			symbol = b.toString();
		}

		// children
		ArrayList<IExpression> children = new ArrayList<IExpression>();
		IExpression child = null;
		do {
			child = parseExpression( s, pos);
			if( child != null ) {
				children.add( child);
				pos = child.getPosition();
			}
		} while(child != null);

		// advance whitespace
		while(Character.isSpaceChar(s.charAt(pos))) {
			++pos;
		}

		if(s.charAt(pos) != ')')
			return null;
		++pos;

		return new ExprProc ( pos, symbol, children );
	}
*/

	private ExprSymbol parseSymbol( String s, int pos)
	{
		// symbol
		int pos2 = pos;
		if( Character.isLetter( peekChar( s,pos2)) || peekChar( s, pos2) == '_' ) {
			while( Character.isLetter( peekChar( s, pos2))
					|| Character.isDigit( peekChar( s, pos2))
					|| peekChar( s, pos2) == '_'
					)
				 {
				++pos2;
			}
			return new ExprSymbol( pos2, s.substring(pos,pos2));
		}
		return null;
	}


	public ExprProc parseFunction( String s, int pos)
	{
		// name
		int pos2 = pos;
		if( Character.isLetter( peekChar( s,pos2)) || peekChar( s, pos2) == '_' ) {
			while( Character.isLetter( peekChar( s, pos2))
					|| Character.isDigit( peekChar( s, pos2))
					|| peekChar( s, pos2) == '_'
			) ++pos2;
		}

		if( pos == pos2)
			return null;

		String name = s.substring( pos, pos2 ); 
		pos = pos2;
		pos = skipWhite( s, pos ); 

		// argument list
		if( peekChar( s, pos) != '(')
			return null;
		++pos;

		ArrayList<IExpression> children = new ArrayList<IExpression>();
		while( pos < s.length() ) {
			IExpression child = parseExpression1( s, pos);
			if( child != null ) {
				children.add( child);
				pos = child.getPosition();
			}

			pos = skipWhite( s, pos ); 

			if( peekChar( s, pos) == ',') {
				++pos;
				continue; 
			}
			else if( peekChar( s, pos) == ')') {
				++pos;
				return new ExprProc( pos, name, children );
			}
			else
				return null;
		} 

		return null;
	}




	private IExpression parseQuotedTimestamp( String s, int pos )
	{
		/*
			might change name to parseStringLiteral, and then have Timestamp as one specialisation ...
		*/
		if( peekChar( s, pos) != '\'')
			return null;
		++pos;
		IExpression expr = parseTimestamp( s, pos );
		if( expr == null)
			return null;
		pos = expr.getPosition();
		if( peekChar( s, pos) != '\'')
			return null;
		++pos;
		return createUnaryExpr( pos, "nop", expr );
	}


	private IExpression parseWKT( String s, int pos )
	{
		// we parse just enough to be able to extract the wkt from an expression
		// TODO we should do the same thing for operator checking...
		// make static
		final String prefixes [] = { 
			"GEOMETRY", "POINT", "LINESTRING", "POLYGON", "MULTIPOINT", "MULTILINESTRING", "MULTIPOLYGON",
			"GEOMETRYCOLLECTION", "CIRCULARSTRING", "COMPOUNDCURVE", "CURVEPOLYGON", "MULTICURVE", "MULTISURFACE",
			"CURVE", "SURFACE", "POLYHEDRALSURFACE", "TIN", "TRIANGLE" 
		}; 

		int pos2 = pos;
		boolean foundPrefix = false;
		for( String prefix : prefixes )
		{
			if( pos2 + prefix.length() < s.length() 
				&& s.substring( pos2, pos2 + prefix.length()).equals(prefix )) {
				pos2 += prefix.length(); 
				foundPrefix = true;
				break;
			}	
		}
		if( !foundPrefix ) 
			return null;

		pos2 = skipWhite( s, pos2); 
		if( peekChar( s, pos2 ) != '(' )
			return null;
		++pos2;

		int depth = 1;
		while( pos2 < s.length() ) 
		{
			if( peekChar( s, pos2 ) == '(' )
				++depth;
			else if( peekChar( s, pos2 ) == ')' ) {
				--depth;
				if( depth == 0 ) {
					++pos2;
					return new ExprWKTLiteral(pos2, s.substring( pos , pos2));
				}
			}	
			++pos2;
		}
		return null;
	}


	private ExprTimestampLiteral parseTimestamp( String s, int pos )
	{
		// eg. if it looks like a date
		int pos2 = pos;
		while(
			Character.isDigit( peekChar( s, pos2))
			|| peekChar( s, pos2) == '-'
			|| peekChar( s, pos2) == ':'
			|| peekChar( s, pos2) == 'Z'
			|| peekChar( s, pos2) == 'T'
		) ++pos2;

		if(pos != pos2) {
			try {
				String x = s.substring( pos, pos2);
				Timestamp d = new java.sql.Timestamp(df.parse(x).getTime());
				return new ExprTimestampLiteral( pos2, d);
			} catch( Exception e ) {
			}
		}
		return null;
	}

	private ExprIntegerLiteral parseIntegerLiteral( String s, int pos )
	{
		int pos2 = pos;
		while( Character.isDigit( peekChar( s, pos2))) {
			++pos2;
		}

		if( pos != pos2) {
			int value = Integer.parseInt(s.substring(pos, pos2));
			return new ExprIntegerLiteral(pos2, value);
		}
		return null;
	}

	private ExprFloatLiteral parseFloatLiteral(String s, int pos) {
		int pos2 = pos;
		int dotPos = -1;
		boolean hasDot = false;
		while (Character.isDigit( peekChar( s, pos2)) || peekChar(s, pos2) == '.') {
			if (peekChar(s, pos2) == '.') {
				hasDot = true;
				dotPos = pos2;
			}
			++pos2;
		}

		boolean notEmpty = pos != pos2;
		boolean doesntStartWithDot = dotPos != pos;
		boolean doesntEndWithDot = dotPos != (pos2 - 1);

		if (notEmpty && hasDot && doesntStartWithDot && doesntEndWithDot) {
			float value = Float.parseFloat(s.substring(pos, pos2));
			return new ExprFloatLiteral(pos2, value);
		}

		return null;
	}

	private ExprStringLiteral parseStringLiteral( String s, int pos )
	{
		int pos2 = pos;
		if (peekChar(s, pos2) != '\'') {
			return null;
		}
		else {
			++pos2;

			while (peekChar(s, pos2) != '\'') {
				if (peekChar(s, pos2) == -1) {
					return null;
				}
				++pos2;
			}

			++pos2;

			return new ExprStringLiteral(pos2, s.substring(pos + 1, pos2 - 1));
		}
	}
}


