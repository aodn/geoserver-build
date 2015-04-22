
package au.org.emii.ncdfgenerator;

interface IAttributeValueParser
{
	public AttributeValue parse( String s ) throws NcdfGeneratorException; 
}

