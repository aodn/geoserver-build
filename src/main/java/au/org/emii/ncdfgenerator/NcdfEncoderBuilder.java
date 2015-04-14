

package au.org.emii.ncdfgenerator;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream ;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern ;
import java.util.regex.Matcher;
import java.util.AbstractMap.SimpleImmutableEntry;

import java.sql.*;

import java.lang.RuntimeException;

import java.text.SimpleDateFormat;

import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Dimension;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.Index;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;



import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;


interface IVisitor
{
	public void visit( ExprInteger expr );
	public void visit( ExprProc expr );
	public void visit( ExprLiteral expr );
	public void visit( ExprSymbol expr );
	public void visit( ExprTimestamp expr );
}


interface IExpression
{
	public int getPosition() ;
	public void accept( IVisitor v ) ;
}


class ExprSymbol implements IExpression
{
	public ExprSymbol( int pos, String value)
	{
		this.pos = pos;
		this.value = value;
	}

	public int getPosition() { return pos; }
	public void accept( IVisitor v )  { v.visit( this); }

	final int pos;
	final String value;
}


class ExprInteger implements IExpression
{
	public ExprInteger( int pos, int value)
	{
		this.pos = pos;
		this.value = value;
	}

	public int getPosition() { return pos; }
	public void accept( IVisitor v )  { v.visit( this); }

	final int pos;
	final int value;
}


class ExprTimestamp implements IExpression
{
	public ExprTimestamp( int pos, Timestamp value)
	{
		this.pos = pos;
		this.value = value;
	}

	public int getPosition() { return pos; }
	public void accept( IVisitor v )  { v.visit( this); }

	final int pos;
	final Timestamp value;
}


class ExprLiteral implements IExpression
{
	public ExprLiteral( int pos, String value)
	{
		this.pos = pos;
		this.value = value;
	}

	public int getPosition() { return pos; }
	public void accept( IVisitor v )  { v.visit( this); }
	final int pos;
	final String value;
}


class ExprProc implements IExpression
{
	public ExprProc( int pos, String symbol, ArrayList<IExpression> children  )
	{
		this.pos = pos;
		this.symbol = symbol;
		this.children = children;
	}

	public int getPosition() { return pos; }
	public void accept( IVisitor v )  { v.visit( this); }

	final int		pos;
	final String symbol;
	final ArrayList<IExpression> children;
}


interface IExprParser
{
	public IExpression parseExpression(String s, int pos);

}


class ExprParser implements IExprParser
{
	// the input source is actually constant. while the pos needs to be held
	// on the stack
	// potentially we should keep the buffer state around...


	// | Int
	// | f '(' expr_list ')'   f
	// | expr_list =
	//			expr
	// | '(' expr, expr ')'   tuple

	// TODO we should check that we have matched the string entirely
	// with whitespace at the end... parseEOF or similar?

	public ExprParser() {

		df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	// should pass this as a constructor dependency
	final SimpleDateFormat df;

	public IExpression parseExpression(String s, int pos)
	{
		// advance whitespace
		while(Character.isSpaceChar(s.charAt(pos))) {
			++pos;
		}
		// timestamp
		IExpression expr = parseTimestamp(s, pos);
		if(expr != null) {
			System.out.println( "parsed Timestamp" );
			return expr;
		}

		// integer
		expr = parseInt(s, pos);
		if(expr != null)
			return expr;
		// literal
		expr = parseLiteral(s, pos);
		if(expr != null)
			return expr;
		expr = parseSymbol( s, pos );
		if(expr != null)
			return expr;
		// proc
		expr = parseProc(s, pos);
		if(expr != null)
			return expr;
		return null;
	}


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

	private ExprSymbol parseSymbol( String s, int pos)
	{
		// atom....
		// symbol
		int pos2 = pos;
		if(Character.isLetter(s.charAt(pos2)) || s.charAt(pos2) == '_' ) {
			while(Character.isLetter(s.charAt(pos2))
				|| Character.isDigit(s.charAt(pos2))
				|| s.charAt(pos2) == '_') {
				++pos2;
			}
			return new ExprSymbol( pos2, s.substring(pos,pos2));
		}
		return null;
	}


	private ExprTimestamp parseTimestamp( String s, int pos )
	{
		// eg. if it looks like a date
		int pos2 = pos;
		while(Character.isDigit(s.charAt(pos2))
			|| s.charAt(pos2) == '-'
			|| s.charAt(pos2) == ':'
			|| s.charAt(pos2) == 'Z'
			|| s.charAt(pos2) == 'T'
		) ++pos2;

		if(pos != pos2) {
			try {
				String x = s.substring( pos, pos2);
				Timestamp d = new java.sql.Timestamp(df.parse(x).getTime());
				return new ExprTimestamp( pos2, d);
			} catch( Exception e ) {
			}
		}
		return null;
	}

	private ExprInteger parseInt( String s, int pos )
	{
		int pos2 = pos;
		while(Character.isDigit(s.charAt(pos2)))
			++pos2;

		if( pos != pos2) {
			int value = Integer.parseInt(s.substring(pos, pos2));
			return new ExprInteger(pos2, value);
		}
		return null;
	}

	private ExprLiteral parseLiteral( String s, int pos )
	{
		// TODO pos2
		int pos2 = pos;
		// ignore escaping for the moment
		if(s.charAt(pos2) != '\'')
			return null;
		++pos2;

		while(s.charAt(pos2) != '\'')
			++pos2;

		++pos2;
		return new ExprLiteral(pos, s.substring( pos + 1, pos2 - 1));
	}
}


class PrettyPrinterVisitor implements IVisitor
{
	// TODO should take the stream on the constructor

	public void visit( ExprSymbol expr )
	{
		System.out.print( "Symbol:" + expr.value );
	}

	public void visit(  ExprInteger expr )
	{
		System.out.print( "Integer:" + expr.value );
	}

	public void visit( ExprTimestamp expr )
	{
		System.out.print( "Timestamp:" + expr.value );
	}

	public void visit(  ExprLiteral expr )
	{
		System.out.print( "Literal:" + expr.value );
	}

	public void visit( ExprProc expr )
	{
		System.out.print( "(" + expr.symbol + " " );
		for( IExpression child : expr.children ) {
			child.accept(this);
			System.out.print( " ");
		}
		System.out.println( ")" );
	}
}


class PGDialectSelectionGenerator implements IVisitor
{
	StringBuilder b;
	SimpleDateFormat df;


	public PGDialectSelectionGenerator( StringBuilder b )
	{
		this.b = b;
		this.df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	// think our naming is incorrect
	public void visit( ExprInteger expr )
	{
		// This should actually emit a '?' and load the value into the sql parameter list
		// to avoid sql injection
		b.append( expr.value );
	}

	public void visit( ExprTimestamp expr )
	{
		b.append( "'" + df.format(expr.value ) + "'" );
	}

	public void visit( ExprLiteral expr )
	{
		b.append("'"+ expr.value + "'");
	}

	public void visit( ExprSymbol expr )
	{
		// must quote field to enforce full case handling
		b.append('\"' + expr.value + '\"' );
	}

	public void visit( ExprProc expr )
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
			throw new RuntimeException( "Unrecognized proc expression symbol '" + symbol + "'" );
		}
	}

	public void emitInfixSqlExpr( String op, ExprProc expr )
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



interface IDialectTranslate
{
	public String process( IExpression expr ) ;
}


class PGDialectTranslate implements IDialectTranslate
{
	// we have to have something to instantiate the specific visitor
	public PGDialectTranslate( )
	{
		// should pass the PGDialect selction on constructor
		; // this.visitor = visitor;
	}

	public String process( IExpression expr )
	{
		StringBuilder b = new StringBuilder();
		PGDialectSelectionGenerator visitor = new PGDialectSelectionGenerator( b);
		expr.accept( visitor );

		return b.toString();
	}
}


interface IValueEncoder
{
	// Netcdf value encoder from java/sql types

	public void encode( Array A, int ima, Object value );
	public void prepare( Map<String, String> attributes );
	public DataType targetType();
}


class TimestampValueEncoder implements IValueEncoder
{
	TimestampValueEncoder()
	{
		// all the date attribute parsing slows the code a lot so calculate once at init .
		this.epoch = 0;
		this.unit = null;
		this.fill = 1234;
	}

	// need a date unit...
	long epoch;  // in seconds
	String unit; // seconds, days
	float fill;

	public DataType targetType()
	{
		return DataType.FLOAT;
	}

	public void prepare( Map<String, String> attributes )
	{
		Matcher m = Pattern.compile("([a-zA-Z]*)[ ]*since[ ]*(.*)").matcher( attributes.get("units") );
		if(!m.find())
		{
			throw new RuntimeException( "couldn't parse attribute date");
		}
		unit = m.group(1);
		String epochString = m.group(2);
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
			Date ts = df.parse(epochString);
			epoch = (Long) ts.getTime() / 1000 ;
		} catch( Exception e )
		{
			throw new RuntimeException( "couldn't extract timestamp '" + epochString + "' " + e.getMessage()  );
		}

		fill = Float.valueOf( attributes.get( "_FillValue" )).floatValue();
	}

	public void encode( Array A, int ima,  Object value )
	{

		if( value == null) {
			A.setFloat( ima, fill );
		}
		else if( value instanceof java.sql.Timestamp ) {
			long seconds =  ((java.sql.Timestamp)value).getTime() / 1000  ;
			long ret = seconds - epoch;
			if( unit.equals("days"))
				ret /= 86400;
			else if( unit.equals("minutes"))
				ret /= 1440;
			else if ( unit.equals("seconds"))
				;
			else
				throw new RuntimeException( "unrecognized time unit " + unit );

			A.setFloat( ima, (float) ret );
		}
		else {
			throw new RuntimeException( "Not a timestamp" );
		}
	}
}


class FloatValueEncoder implements IValueEncoder
{
	FloatValueEncoder()
	{
		this.fill = 1234;
	}

	float fill;

	public DataType targetType()
	{
		return DataType.FLOAT;
	}

	public void prepare( Map<String, String> attributes )
	{
		fill = Float.valueOf( attributes.get( "_FillValue" )).floatValue();

	}

	public void encode( Array A, int ima, Object value )
	{
		if( value == null) {
			A.setFloat( ima, fill );
		}
		else if( value instanceof Float ) {
			A.setFloat( ima, (Float) value);
		}
		else if( value instanceof Double ) {
			A.setFloat( ima, (float)(double)(Double) value);
		}
		else {
			throw new RuntimeException( "Failed to coerce type '" + value.getClass() + "' to float" );
		}
	}
}


class IntValueEncoder implements IValueEncoder
{
	// Int is 32bit in Netcdf

	IntValueEncoder()
	{
		this.fill = 1234;
	}

	int fill;

	public DataType targetType()
	{
		return DataType.INT;
	}

	public void prepare( Map<String, String> attributes )
	{
		fill = Integer.valueOf( attributes.get( "_FillValue" )).intValue();
	}

	public void encode( Array A, int ima, Object value )
	{
		if( value == null) {
			A.setInt( ima, fill );
		}
		else if( value instanceof Integer ) {
			A.setInt( ima, (Integer) value);
		}
		else if( value instanceof Long ) {
			A.setInt( ima, (int)(long)(Long) value);
		}
		else {
			throw new RuntimeException( "Failed to coerce type '" + value.getClass() + "' to float" );
		}
	}
}


class ByteValueEncoder implements IValueEncoder
{
	ByteValueEncoder()
	{
		this.fill = 0x0;
	}
	byte fill;

	// assumption that the Object A is a float array
	public DataType targetType()
	{
		return DataType.BYTE;
	}

	public void prepare( Map<String, String> attributes )
	{
		// eg. no unsigned byte in java, so use integer and downcast
		fill = (byte) Integer.decode( attributes.get( "_FillValue" ) ).intValue();
	}

	public void encode( Array A, int ima, Object value )
	{
		if( value == null) {
			A.setByte( ima, fill );
		}
		else if(value instanceof Byte)
		{
			A.setByte( ima, (Byte) value);
		}
		else if(value instanceof String && ((String)value).length() == 1) {
			// coerce string of length 1 to byte
			String s = (String) value;
			Byte ch = s.getBytes()[0];
			A.setByte(ima, ch);
		}
		else {
			throw new RuntimeException( "Failed to convert type to byte");
		}
	}
}


interface IAddValue
{
	// change name to put(), or append? and class to IBufferAddValue
	public void addValueToBuffer( Object value );
}

interface IVariableEncoder extends IAddValue
{

	public void define( NetcdfFileWriteable writer ) ;
	public void finish( NetcdfFileWriteable writer) throws Exception ;

	public void addValueToBuffer( Object value );

	public String getName(); // change class name to IVariableEncoder and this to just getName()

}


interface IDimension extends IAddValue
{
	public void define( NetcdfFileWriteable writer) ;

	public Dimension getDimension( ) ; // horrible to expose this...
										// can't the caller create the dimension?
	//
	public int getLength();

	public void addValueToBuffer( Object value );

	public String getName();

}



class DimensionImpl implements IDimension
{

	public DimensionImpl( String name )
	{
		this.name = name; // required to encode dimension
		this.size = 0;
	}

	final String name;
	int size;
	Dimension dimension;


	public Dimension getDimension( )  // bad naming
	{
		// throw if not defined...
		return dimension;
	}

	public int getLength()
	{
		return size;
	}

	public void define( NetcdfFileWriteable writer)
	{
		// System.out.println( "** before writing dimension " + name + " " + size );

		dimension = writer.addDimension( name, size );
		//return null;
	}

	public void addValueToBuffer( Object value )
	{
		++size;
	}

	public String getName() { return name ; }

	{
		System.out.println( "** Dimension size " + size );
	}
}



class VariableEncoder implements IVariableEncoder
{
	public VariableEncoder(
		String variableName,
		ArrayList< IDimension> dimensions,
		IValueEncoder encodeValue,
		Map<String, String> attributes
	) {
		this.variableName = variableName;
		this.encodeValue = encodeValue;
		this.attributes = attributes;
		this.dimensions = dimensions;
		this.buffer = new ArrayList<Object>( );
	}

	final String variableName;
	final IValueEncoder			encodeValue;
	final Map<String, String>	attributes;
	final ArrayList<IDimension>	dimensions; // change name childDimensions
	final ArrayList<Object>		buffer;


	/*	we can also record the table, or index of table here if we want
			to incorporate into the strategy.
		eg. we can compre with xml to decide what to do.
	*/
	public void addValueToBuffer( Object value )
	{
		// perhaps delegate to strategy...
		buffer.add( value );
	}

	public void define( NetcdfFileWriteable writer )
	{
		// write dims and attributes

		// make sure children are defined already
		List<Dimension> d = new ArrayList<Dimension>();
		for( IDimension dimension: dimensions)
		{
			d.add( dimension.getDimension() );
		}

		writer.addVariable(variableName, encodeValue.targetType(), d );

		for( Map.Entry< String, String> entry : attributes.entrySet()) {
			writer.addVariableAttribute( variableName, entry.getKey(), entry.getValue()/*.toString()*/ );
		}
	}


	public void writeValues( ArrayList<IDimension> dims, int dimIndex, int acc, Array A  )
	{
		if( dimIndex < dims.size() )
		{
			Dimension dim = dims.get( dimIndex ).getDimension();
			for( int i = 0; i < dim.getLength(); i++ )
			{
				writeValues( dims, dimIndex + 1, acc + i, A );
			}
		}
		else
		{
			// System.out.println( "dimIndex " + "  acc " + acc  + "  buffer " + buffer.get( acc ) );
			// public void encode( Array A, int ima, Map<String, Object> attributes, Object value );

			encodeValue.encode( A, acc, buffer.get( acc ) );

			// A.setFloat( acc, (float) 99999. );
		}

	}

	private static int[] toIntArray( List<Integer> list)
	{
		// List.toArray() only supports java Boxed Integers...
		int[] ret = new int[list.size()];
		for(int i = 0;i < ret.length;i++)
			ret[i] = list.get(i);
		return ret;
	}

	public void finish( NetcdfFileWriteable writer) throws Exception
	{

		System.out.println( "finish " + variableName );

		ArrayList< Integer> shape = new ArrayList< Integer>() ;
		for( IDimension dimension : dimensions ) {
			shape.add( dimension.getLength() );
		}

		Array A = Array.factory( encodeValue.targetType(), toIntArray(shape ) );

		encodeValue.prepare( attributes );

		writeValues( dimensions,  0, 0 , A );

		// int [] origin = new int[1];
		// writer.write(variableName, origin, A);
		writer.write(variableName, A);
	}


	public String getName()
	{
		return variableName;
	}
}


interface ICreateWritable
{
	public NetcdfFileWriteable create( )  throws IOException ;
}


class CreateWritable implements  ICreateWritable
{
	// NetcdfFileWriteable is not an abstraction over a stream!. instead it insists on being a file...

	public NetcdfFileWriteable create() throws IOException
	{
		System.out.println( "creating writer" );
		// netcdf stuff
		String filename = "testWrite.nc";

		return NetcdfFileWriteable.createNew(filename, false);
	}

	// TODO method to request as a byte stream and return?
	// public getByteStream () { }
}


class NodeWrapper implements Iterable<Node> 
{
	// just a helper class

    private Node node;
    private List<Node> nodes;
    private NodeList nodeList;

    public NodeWrapper(Node node) {
        this.node = node;
    }

    public Iterator<Node> iterator() {
        if (nodes == null) {
            buildNodes();
        }

        return nodes.iterator();
    }

    private void buildNodes() {
        nodes = new ArrayList<Node>(getListLength());
        for (int i = 0; i < getListLength(); i++) {
            nodes.add(nodeList.item(i));
        }
    }

    private int getListLength() {
        return getNodeList().getLength();
    }

    private NodeList getNodeList() {
        if (nodeList == null) {
            setNodeList();
        }
        return nodeList;
    }

    private void setNodeList() {
        if (node.hasChildNodes()) {
            nodeList = node.getChildNodes();
        }
        else {
            nodeList = new NullNodeList();
        }
    }

    private class NullNodeList implements NodeList {

        public Node item(int index) {
            return null;
        }

        public int getLength() {
            return 0;
        }
    }
}


class NcdfDefinition
{
	NcdfDefinition(
		String schema,
		String virtualDataTable,
		String virtualInstanceTable,
		Map< String, IDimension> dimensions,
		Map< String, IVariableEncoder> encoders
	) {
		this.schema = schema;
		this.virtualDataTable = virtualDataTable;
		this.virtualInstanceTable = virtualInstanceTable;
		this.dimensions = dimensions;
		this.encoders = encoders;
	}

	final String schema;
	final String virtualDataTable;
	final String virtualInstanceTable;
	final Map< String, IDimension> dimensions;
	final Map< String, IVariableEncoder> encoders;
}


class NcdfDefinitionXMLParser
{
	private boolean isNodeName( Node node, String name )
	{
		return node.getNodeType() == Node.ELEMENT_NODE
			&& node.getNodeName().equals( name );
	}

	private String nodeVal( Node node  )
	{
		Node child = node.getFirstChild();
		if( child != null && child.getNodeType() == Node.TEXT_NODE )
			return child.getNodeValue();

		return "";
	}


	private Map< String, String> parseKeyVals( Node node )
	{
		// have another version that does this for attributes
		Map< String, String> m = new HashMap< String, String>();
		for( Node child : new NodeWrapper(node) )
			if( child.getNodeType() == Node.ELEMENT_NODE )
				m.put( child.getNodeName(), nodeVal( child ) );

		// add any xml attributes
		NamedNodeMap attrs = node.getAttributes();
		for( int i = 0; i < attrs.getLength(); ++i )
		{
			Node child = attrs.item( i) ;
			m.put( child.getNodeName(), nodeVal( child ) );
		}

		return m;
	}

	private IDimension parseDimension( Node node)
	{
		if( isNodeName( node, "dimension")) {
			Map< String, String> m = parseKeyVals( node );
			return new DimensionImpl( m.get( "name" ) );
		}
		return null;
	}

	// having a simple parse key-vals function, means we can do it with attributes as alternative syntax.

	private Map< String, IDimension> parseDimensions( Node node )
	{
		if( isNodeName( node, "dimensions"))
		{
			Map< String, IDimension> dimensions = new HashMap< String, IDimension> () ;
			for( Node child : new NodeWrapper(node) ) {
				IDimension dimension = parseDimension( child );
				if( dimension != null)
					dimensions.put( dimension.getName(), dimension );
			}
			return dimensions;
		}
		return null;
	}


	private IValueEncoder parseEncoder( Node node)
	{
		if( isNodeName( node, "encoder"))
		{
			String val = nodeVal( node );
			if( val.equals( "integer")) {

				System.out.println( "WHOOT WHOOT WHOOT" );

				return new IntValueEncoder();

				// throw new RuntimeException( "INT" );
			}
			else if( val.equals( "float")) {
				return new FloatValueEncoder();
			}
			else if( val.equals( "byte")) {
				return new ByteValueEncoder();
			}
			else if( val.equals( "time")) {
				return new TimestampValueEncoder();
			}
			else
			{
				throw new RuntimeException( "Unrecognized value type encoder" );
			}
		}
		return null;
	}


	private SimpleImmutableEntry<String, String> parseAttribute( Node node )
	{

		if( isNodeName( node, "attribute"))
		{
			Map< String, String> m = parseKeyVals( node );
			String key = m.get("name");
			String val = m.get("value");
			return new SimpleImmutableEntry< String, String>( key, val );
		}
		return null;
	}


	private Map<String, String> parseAttributes( Node node )
	{
		if( isNodeName( node, "attributes"))
		{
			Map<String, String> m = new HashMap<String, String> ();
			for( Node child : new NodeWrapper(node) ) {
				SimpleImmutableEntry< String, String> pair = parseAttribute( child);
				if( pair != null)
					m.put( pair.getKey(), pair.getValue());
			}
			return m;
		}
		return null;
	}


	private IDimension parseDimensionRef ( Node node, Map< String, IDimension> dimensionsContext )
	{
		if( isNodeName( node, "dimension")) {
			Map< String, String> m = parseKeyVals( node );
			// System.out.println( "found dimension ref " + m.get( "name" ) );
			return dimensionsContext.get( m.get( "name" ));
		}
		return null;
	}


	private Map< String, IDimension> parseDimensionsRef( Node node, Map< String, IDimension> dimensionsContext )
	{
		if( isNodeName( node, "dimensions"))
		{
			Map< String, IDimension> dimensions = new HashMap< String, IDimension> () ;
			for( Node child : new NodeWrapper(node) ) {
				IDimension dimension = parseDimensionRef( child, dimensionsContext);
				if( dimension != null)
					dimensions.put( dimension.getName(), dimension );
			}
			return dimensions;
		}
		return null;
	}

	// think we may want a more general context ...

	private IVariableEncoder parseVariableEncoder( Node node, Map< String, IDimension> dimensionsContext  )
	{
		String name = null;
		Map< String, IDimension> dimensions = null;
		IValueEncoder encodeValue = null;
		Map< String, String> attributes = null;

		if( isNodeName( node, "variable"))
		{
			for( Node child : new NodeWrapper(node) )
			{
				// this is very neat. may want to do this explicitly rather than using the map...
				if( isNodeName( child, "name" ))
					name = nodeVal( child);
				if( dimensions == null)
					dimensions = parseDimensionsRef( child, dimensionsContext );
				if( encodeValue == null)
					encodeValue = parseEncoder( child ) ;
				if( attributes == null)
					attributes = parseAttributes( child );
			}

			if( dimensions == null )
			{
				dimensions = new HashMap< String, IDimension> ();
			}

			if( name != null
				&& encodeValue != null
				&& attributes != null )
			{
				System.out.println( "whoot creating encoder " + name  );

				return new VariableEncoder ( name , new ArrayList<IDimension>(dimensions.values()), encodeValue , attributes ) ;
			}
			else {
				throw new RuntimeException("missing something  " );
				// return null;
			}
		}
		return null;
	}


	private Map< String, IVariableEncoder> parseVariableEncoders( Node node, Map< String, IDimension> dimensionsContext  )
	{
		if( isNodeName( node, "variables"))
		{
			Map< String, IVariableEncoder> m = new HashMap < String, IVariableEncoder>();
			for( Node child : new NodeWrapper(node)) {
				IVariableEncoder e = parseVariableEncoder( child, dimensionsContext  );
				if( e != null )
					m.put( e.getName(), e );
			}
			return m;
		}
		return null;
	}

	private Map< String, String> parseSource( Node node )
	{
		if( isNodeName( node, "source")) /// data or dataSource
		{
			Map< String, String> source = parseKeyVals( node);

			/*
			System.out.println( "*** GOT CONFIG " +  source.size()   );
			for( Map.Entry< String, String> entry : source.entrySet()) {
				System.out.println( "*** " + entry.getKey() + " " + entry.getValue() );

			} */
			return source;
		}
		return null;

	}

	NcdfDefinition parseDefinition( Node node )
	{
		// think we need a context?
		if( isNodeName( node, "definition"))
		{
			Map< String, String> source = null;
			Map< String, IDimension> dimensions = null;
			Map< String, IVariableEncoder> encoders = null;

			// pick out dimensions
			for( Node child : new NodeWrapper(node)) {

				if( source == null )
					source = parseSource( child );

				if( dimensions == null )
					dimensions = parseDimensions( child );

				if( encoders == null )
					encoders = parseVariableEncoders( child, dimensions );
			}

			String schema = source.get( "schema" );
			String virtualDataTable = source.get( "virtualDataTable" );
			String virtualInstanceTable =source.get( "virtualInstanceTable" );

			return new NcdfDefinition( schema, virtualDataTable, virtualInstanceTable, dimensions, encoders );
		}
		return null;
	}
}


class NcdfEncoder
{
	final IExprParser exprParser;				
	final IDialectTranslate translate ;		
	final Connection conn;
	final ICreateWritable createWritable; // generate a writable
	final NcdfDefinition definition ;
	final String filterExpr;

	final int fetchSize;
	IExpression selection_expr;
	ResultSet featureInstancesRS;

	public NcdfEncoder(
		IExprParser exprParser,
		IDialectTranslate translate,
		Connection conn,
		ICreateWritable createWritable,
		NcdfDefinition definition,
		String filterExpr
	) {
		this.exprParser = exprParser;
		this.translate = translate; // sqlEncode.. dialect... specialization
		this.conn = conn;
		this.createWritable = createWritable;
		this.definition = definition;
		this.filterExpr = filterExpr;

		fetchSize = 1000;
		featureInstancesRS = null;
		selection_expr = null;
	}

	public void prepare() throws Exception
	{
		selection_expr = exprParser.parseExpression( filterExpr, 0);
		// bad, should return expr or throw
		if(selection_expr == null) {
			throw new RuntimeException( "failed to parse expression" );
		}

		System.out.println( "setting search_path to " + definition.schema );

		PreparedStatement s = conn.prepareStatement("set search_path='" + definition.schema + "'");
		// PreparedStatement s = conn.prepareStatement("set search_path='" + schema + "',public");
		// PreparedStatement s = conn.prepareStatement("set search_path=" + schema + ",public");
		s.execute();
		s.close();

		String selection = translate.process( selection_expr);

		String query = "SELECT distinct data.instance_id  FROM (" + definition.virtualDataTable + ") as data where " + selection + ";" ;
		System.out.println( "first query " + query  );

		PreparedStatement stmt = conn.prepareStatement( query );
		stmt.setFetchSize(fetchSize);

		// try ...
		// change name featureInstancesRSToProcess ?
		featureInstancesRS = stmt.executeQuery();
		System.out.println( "done determining feature instances " );
		// should determine our target types here
	}

	public void populateValues(
		Map< String, IDimension> dimensions,
		Map< String, IVariableEncoder> encoders,
		String query
		)  throws Exception
	{
		System.out.println( "query " + query  );

		// sql stuff
		PreparedStatement stmt = conn.prepareStatement( query );
		stmt.setFetchSize(fetchSize);
		ResultSet rs = stmt.executeQuery();

		// now we loop the main attributes
		ResultSetMetaData m = rs.getMetaData();
		int numColumns = m.getColumnCount();

		// pre-map the encoders by index according to the column name
		ArrayList< IAddValue> [] processing = (ArrayList< IAddValue> []) new ArrayList [numColumns + 1];

		for ( int i = 1 ; i <= numColumns ; i++ ) {
			// System.out.println( "column name "+ m.getColumnName(i) );
			processing[i] = new ArrayList< IAddValue> ();

			IDimension dimension = dimensions.get( m.getColumnName(i));
			if( dimension != null)
				processing[i].add( dimension );

			IAddValue encoder = encoders.get(m.getColumnName(i));
			if( encoder != null)
				processing[i].add( encoder );
		}

		// process result set rows
		while ( rs.next() ) {
			for ( int i = 1 ; i <= numColumns ; i++ ) {
				for( IAddValue p : processing[ i] ) {
					p.addValueToBuffer( rs.getObject( i));
				}
			}
		}
	}


	public NetcdfFileWriteable get() throws Exception
	{
		// TODO should just return a readable IStream, client shouldn't care that it's netcdf type. 

		try { 
			if( featureInstancesRS.next()) 
			{
				// munge
				long instance_id = -1234;
				Object o = featureInstancesRS.getObject(1);
				Class clazz = o.getClass();
				if( clazz.equals( Integer.class )) {
					instance_id = (long)(Integer)o;
				}
				else if( clazz.equals( Long.class )) {
					instance_id = (long)(Long)o;
				} else {
					throw new RuntimeException( "Can't convert intance_id type to integer" );
				}

				System.out.println( "whoot get(), instance_id is " + instance_id );

				String selection = translate.process( selection_expr); // we ought to be caching the specific query ???

				populateValues( definition.dimensions, definition.encoders, 
					"SELECT * FROM (" + definition.virtualInstanceTable + ") as instance where instance.id = " + Long.toString( instance_id) );


				// is the order clause in sql part of projection or selection ?

				// eg. concat "," $ map (\x -> x.getName) dimensions.values ...
				String dimensionVar = "";
				for( IDimension dimension : definition.dimensions.values() )
				{
					if( ! dimensionVar.equals("")){
						dimensionVar += ",";
					}
					dimensionVar += "\"" + dimension.getName() + "\"" ;
				}

				populateValues( definition.dimensions, definition.encoders, 
					"SELECT * FROM (" + definition.virtualDataTable + ") as data where " + selection +  " and data.instance_id = " + Long.toString( instance_id) + " order by " + dimensionVar  );

				NetcdfFileWriteable writer = createWritable.create();


				for ( IDimension dimension: definition.dimensions.values()) {
					dimension.define(writer);
				}

				for ( IVariableEncoder encoder: definition.encoders.values()) {
					encoder.define( writer );
				}
				// finish netcdf definition
				writer.create();

				for ( IVariableEncoder encoder: definition.encoders.values()) {
					// maybe change name writeValues
					encoder.finish( writer );
				}
				// write the file
				writer.close();

				// TODO we should be returning a filestream here...
				// the caller doesn't care that it's a netcdf
				return writer;
			}
			else {
				// no more netcdfs
				conn.close();
				return null;
			}
		} catch ( Exception e ) {
			System.out.println( "Opps " + e.getMessage() ); 
			conn.close();
			return null;
		} 
	}
}


public class NcdfEncoderBuilder
{

	public NcdfEncoderBuilder()
	{ }


	public NcdfEncoder create ( InputStream config, String filterExpr, Connection conn ) throws Exception
	{
		// not sure if the expression parsing shouldn't go in here?
		// not sure if definition decoding should be done here...

		NcdfDefinition definition = null;
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
			Node node =	document.getFirstChild();
			definition = new NcdfDefinitionXMLParser().parseDefinition( node );

		} finally {
			config.close();
		}

		IExprParser parser = new ExprParser();
		IDialectTranslate translate = new  PGDialectTranslate();
		ICreateWritable createWritable = new CreateWritable();

		NcdfEncoder generator = new NcdfEncoder( parser, translate, conn, createWritable, definition, filterExpr );
		// think client should probably call prepare()
		generator.prepare();
		return generator;
	}
}


