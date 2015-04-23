
package au.org.emii.ncdfgenerator;


import java.util.List;
import java.util.ArrayList;

import java.sql.Connection;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import java.lang.Exception ;

import au.org.emii.ncdfgenerator.IDimension;
import au.org.emii.ncdfgenerator.NcdfGeneratorException;


class Helper
{
	static String nodeVal( Node node )
	{
		Node child = node.getFirstChild();
		if( child != null && child.getNodeType() == Node.TEXT_NODE )
			return child.getNodeValue();
		return "";
	}

	static boolean isElementNode( Node node )
	{
	    return node.getNodeType() == Node.ELEMENT_NODE;
	}

/*
	static boolean isTextNode( Node node )
	{
	    return node.getNodeType() == Node.TEXT_NODE;
	}
*/
}


class NcdfDefinitionXMLParser
{
	class Context
	{
		// required to resolve references to dimensions from the variable definitions
		List< IDimension> dimensions;

		IDimension getDimensionByName( String name )
		{
			for( IDimension dimension : dimensions ) {
				if( dimension.getName().equals( name))
					return dimension;
			}
			return null;
		}
	}


	class ParseDataSource
	{
		String schema;
		String virtualDataTable;
		String virtualInstanceTable;

		private void extractValue( Node child ) throws NcdfGeneratorException
		{
			String tag = child.getNodeName();
			if( tag.equals( "schema" ))
				schema = Helper.nodeVal( child );
			else if ( tag.equals( "virtualDataTable" ))
				virtualDataTable = Helper.nodeVal( child );
			else if ( tag.equals( "virtualInstanceTable" ))
				virtualInstanceTable = Helper.nodeVal( child );
			else
				throw new NcdfGeneratorException( "Unrecognized tag" );
		}

		DataSource parse( Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "source" ))
				throw new NcdfGeneratorException( "Not source node" );

			for( Node child : new NodeWrapper(node) )
				if( Helper.isElementNode( child))
					extractValue( child );

			return new DataSource( schema, virtualDataTable, virtualInstanceTable );
		}
	}


	class ParseDimension
	{
		IDimension parse( Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "dimension" ))
				throw new NcdfGeneratorException( "Not a dimension node" );

			String name = "";
			NamedNodeMap attrs = node.getAttributes();
			for( int i = 0; i < attrs.getLength(); ++i )
			{
				Node child = attrs.item( i) ;
				if( child.getNodeName().equals( "name" ))
					name = Helper.nodeVal( child);
		        else
					throw new NcdfGeneratorException( "Unrecognized tag" );
			}

			return new DimensionImpl( name );
		}
	}


	class ParseDimensions
	{
		List< IDimension> parse( Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "dimensions" ))
				throw new NcdfGeneratorException( "Not a dimensions node" );

			List< IDimension> dimensions = new ArrayList< IDimension>();
			for( Node child : new NodeWrapper(node) )
			{
				if( Helper.isElementNode( child)) {
					String tag = child.getNodeName();
					if( tag.equals( "dimension" ))
						dimensions.add( new ParseDimension().parse( child ) );
					else
						throw new NcdfGeneratorException( "Unrecognized tag" );
				}
			}
			return dimensions;
		}
	}


	class ParseVariableDimension
	{
		IDimension parse( Context context, Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "dimension" ))
				throw new NcdfGeneratorException( "Not a dimension" );

			NamedNodeMap attrs = node.getAttributes();
			for( int i = 0; i < attrs.getLength(); ++i )
			{
				Node child = attrs.item( i) ;
				if( child.getNodeName().equals( "name" ))
					return context.getDimensionByName( Helper.nodeVal( child) );
				else
					throw new NcdfGeneratorException( "Unrecognized tag" );
			}
			return null;
		}
	}


	class ParseVariableDimensions
	{
		List< IDimension> parse( Context context, Node node  ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "dimensions" ))
				throw new NcdfGeneratorException( "Not a dimensions node" );

			List< IDimension> dimensions = new ArrayList< IDimension>();
			for( Node child : new NodeWrapper(node) )
			{
				if( Helper.isElementNode( child)) {
					String tag = child.getNodeName();
					if( tag.equals( "dimension" ))
						dimensions.add( new ParseVariableDimension().parse( context, child ) );
					else
						throw new NcdfGeneratorException( "Unrecognized tag" );
				}
			}
			return dimensions;
		}
	}


	class ParseAttribute
	{
		String name;
		String value;
		String sql;

		private void extractValue( Node child) throws NcdfGeneratorException
		{
			// attr or node
			String tag = child.getNodeName();
			if( tag.equals( "name" ))
				name = Helper.nodeVal( child);
			else if( tag.equals( "value" ))
				value = Helper.nodeVal( child);
			else if( tag.equals( "sql" ))
				sql = Helper.nodeVal( child);
		}

		Attribute parse( Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "attribute" ))
				throw new NcdfGeneratorException( "Not an attribute" );

			// extract from nested tags
			for( Node child : new NodeWrapper(node) )
				extractValue( child );

			// extract from attributes
			NamedNodeMap attrs = node.getAttributes();
			for( int i = 0; i < attrs.getLength(); ++i )
				extractValue( attrs.item( i));

			return new Attribute( name, value, sql );
		}
	}


	class ParseAttributes
	{
		List< Attribute> parse( Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "attributes" )
				&& !node.getNodeName().equals( "globalattributes" )
			)
				throw new NcdfGeneratorException( "Not an attributes node" );

			List< Attribute> attributes = new ArrayList< Attribute>();
			for( Node child : new NodeWrapper(node) )
			{
				if( Helper.isElementNode( child)) {
					String tag = child.getNodeName();
					if( tag.equals( "attribute" ))
						attributes.add( new ParseAttribute().parse( child ));
					else
						throw new NcdfGeneratorException( "Unrecognized tag" );
				}
			}
			return attributes;
		}
	}


	class ParseEncoder
	{
		IValueEncoder parse( Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "encoder" ))
				throw new NcdfGeneratorException( "Not an encoder" );

			// if we need more encoder detail, then can deal with separately
			String tag = Helper.nodeVal( node );
			if( tag.equals( "integer"))
				return new IntValueEncoder();
			else if( tag.equals( "float"))
				return new FloatValueEncoder();
			else if( tag.equals( "double"))
				return new DoubleValueEncoder();
			else if( tag.equals( "byte"))
				return new ByteValueEncoder();
			else if( tag.equals( "time"))
				return new TimestampValueEncoder();
			else
				throw new NcdfGeneratorException( "Unrecognized tague type encoder '" + tag + "'" );
		}
	}


	class ParseVariable
	{
		IVariableEncoder parse( Context context, Node node  ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "variable" ))
				throw new NcdfGeneratorException( "Not a variable" );

			String name = null;
			IValueEncoder encoder = null;
			List<IDimension> dimensions = new ArrayList< IDimension>(); // support missing dimensions and attributes
			List<Attribute> attributes = new ArrayList< Attribute>();

			for( Node child : new NodeWrapper(node) )
			{
				if( Helper.isElementNode( child)) {
					String tag = child.getNodeName();
					if( tag.equals( "name" ))
						name = Helper.nodeVal( child );
					else if( tag.equals( "encoder" ))
						encoder = new ParseEncoder().parse( child );
					else if( tag.equals( "dimensions" ))
						dimensions = new ParseVariableDimensions().parse( context, child );
					else if (tag.equals( "attributes"))
						attributes = new ParseAttributes().parse( child );
					else
						throw new NcdfGeneratorException( "Unrecognized tag" );
				}
			}

			return new VariableEncoder( name, dimensions, encoder, attributes );
		}
	}


	class ParseVariables
	{
		List<IVariableEncoder> parse( Context context, Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "variables" ))
				throw new NcdfGeneratorException( "Not variables" );

			List<IVariableEncoder> variables = new ArrayList< IVariableEncoder>();
			for( Node child : new NodeWrapper(node) )
			{
				if( Helper.isElementNode( child)) {
					String tag = child.getNodeName();
					if( tag.equals( "variable" ))
						variables.add( new ParseVariable().parse( context, child ) );
					else
						throw new NcdfGeneratorException( "Unrecognized tag" );
				}
			}
			return variables;
		}
	}


	class ParseDefinition
	{
		NcdfDefinition parse( Node node ) throws NcdfGeneratorException
		{
			if( !node.getNodeName().equals( "definition" ))
				throw new NcdfGeneratorException( "Not definition" );

			DataSource dataSource = null; ;
			List<IVariableEncoder> variables = null;
			List< Attribute> globalAttributes = null;
	
			Context context = new Context ();

			for( Node child : new NodeWrapper(node) )
			{
				if( Helper.isElementNode( child)) {
					String tag = child.getNodeName();
					if( tag.equals( "source" ))
						dataSource = new ParseDataSource().parse( child ) ;
					else if( tag.equals( "dimensions" ))
						context.dimensions = new ParseDimensions().parse( child);
					else if( tag.equals( "variables" ))
						variables = new ParseVariables().parse( context, child);
					else if( tag.equals( "globalattributes" ))
						globalAttributes = new ParseAttributes().parse( child);
					else
						throw new NcdfGeneratorException( "Unrecognized tag '" + tag + "'" );
				}
			}

			return new NcdfDefinition( dataSource, globalAttributes, context.dimensions, variables );
		}
	}


	NcdfDefinition parse( Node node ) throws NcdfGeneratorException
	{
		if( node.getNodeName().equals( "definition" ))
			return new ParseDefinition().parse( node );
		else
			throw new NcdfGeneratorException( "Missing definition" );
	}
}

