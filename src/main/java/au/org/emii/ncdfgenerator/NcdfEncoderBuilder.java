
package au.org.emii.ncdfgenerator;

import au.org.emii.ncdfgenerator.cql.ExprParser; 
import au.org.emii.ncdfgenerator.cql.IExprParser; 
import au.org.emii.ncdfgenerator.cql.IDialectTranslate; 

import au.org.emii.ncdfgenerator.cql.PGDialectTranslate; 

import java.sql.Connection;
import java.io.InputStream ;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;


public class NcdfEncoderBuilder
{
	// class responsible for assembling the NcdfEncoder

	final IExprParser parser;
	final IDialectTranslate translate;
	final ICreateWritable createWritable;

	public NcdfEncoderBuilder()
	{
		this.parser = new ExprParser();
		this.translate = new PGDialectTranslate();
		this.createWritable = new CreateWritable( "testWrite.nc");
	}

	public NcdfEncoder create( InputStream config, String filterExpr, Connection conn ) throws Exception
	{
		// not sure if the expression parsing shouldn't go in here?
		// not sure if definition decoding should be done here...

		NcdfDefinition definition = null;
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
			Node node = document.getFirstChild();
			definition = new NcdfDefinitionXMLParser().parseDefinition( node );

		} finally {
			config.close();
		}


		NcdfEncoder generator = new NcdfEncoder( parser, translate, conn, createWritable, definition, filterExpr );
		// should client call prepare() ?
		generator.prepare();
		return generator;
	}
}


