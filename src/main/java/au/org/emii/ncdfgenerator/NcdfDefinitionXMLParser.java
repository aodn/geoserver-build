
package au.org.emii.ncdfgenerator;

import au.org.emii.ncdfgenerator.IVariableEncoder;
import au.org.emii.ncdfgenerator.VariableEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;


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
		throws NcdfGeneratorException
	{
		if( isNodeName( node, "encoder"))
		{
			String val = nodeVal( node );
			if( val.equals( "integer")) {
				return new IntValueEncoder();
			}
			else if( val.equals( "float")) {
				return new FloatValueEncoder();
			}
			else if( val.equals( "double")) {
				return new DoubleValueEncoder();
			}
			else if( val.equals( "byte")) {
				return new ByteValueEncoder();
			}
			else if( val.equals( "time")) {
				return new TimestampValueEncoder();
			}
			else
			{
				// int lineNo = node.getLineNumber();
				throw new NcdfGeneratorException( "Unrecognized value type encoder '" + val + "'" );
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
		throws NcdfGeneratorException
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

				return new VariableEncoder( name , new ArrayList<IDimension>(dimensions.values()), encodeValue , attributes ) ;
			}
			else {
				throw new NcdfGeneratorException( "missing something" );
			}
		}
		return null;
	}


	private Map< String, IVariableEncoder> parseVariableEncoders( Node node, Map< String, IDimension> dimensionsContext  )
		throws NcdfGeneratorException
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
			for( Map.Entry< String, String> entry : source.entrySet()) {
				System.out.println( "*** " + entry.getKey() + " " + entry.getValue() );

			} */
			return source;
		}
		return null;

	}

	NcdfDefinition parseDefinition( Node node )
		throws NcdfGeneratorException
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

