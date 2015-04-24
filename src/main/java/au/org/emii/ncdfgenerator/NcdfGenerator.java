
package au.org.emii.ncdfgenerator;

import au.org.emii.ncdfgenerator.cql.ExprParser;
import au.org.emii.ncdfgenerator.cql.IExprParser;
import au.org.emii.ncdfgenerator.cql.IDialectTranslate;
import au.org.emii.ncdfgenerator.cql.PGDialectTranslate;

import java.sql.Connection;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;


public class NcdfGenerator
{
	final IExprParser parser;
	final IDialectTranslate translate;
	final ICreateWritable createWritable;
	final String layerConfigDir;

	public NcdfGenerator( String layerConfigDir, String tmpCreationDir  )
	{
		this.parser = new ExprParser();
		this.translate = new PGDialectTranslate();
		this.createWritable = new CreateWritable( tmpCreationDir );
		this.layerConfigDir = layerConfigDir;
	}

	public void write( String typename, String filterExpr, Connection conn, OutputStream os ) throws Exception
	{
		NcdfDefinition definition = null;
		InputStream	config = null;

		try {
			config = new FileInputStream( layerConfigDir + "/" + typename + ".xml" );

			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config);
			Node node = document.getFirstChild();
			definition = new NcdfDefinitionXMLParser().parse( node );

		} finally {
			config.close();
		}

		try {
			NcdfEncoder encoder = new NcdfEncoder( parser, translate, conn, createWritable, definition, filterExpr );
			ZipCreator zipCreator = new ZipCreator( encoder);
			encoder.prepare();
			zipCreator.doStreaming( os );
		} finally {
			os.close();
		}
	}
}

