
package au.org.emii.ncdfgenerator;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFileWriteable;
import ucar.ma2.Array;

import au.org.emii.ncdfgenerator.INcdfEncoder;
import au.org.emii.ncdfgenerator.cql.IExpression;
import au.org.emii.ncdfgenerator.cql.IExprParser;
import au.org.emii.ncdfgenerator.cql.IDialectTranslate;


class NcdfEncoder implements INcdfEncoder
{
	private final IExprParser exprParser;
	private final IDialectTranslate translate ;
	private final Connection conn;
	private final ICreateWritable createWritable;
	private final NcdfDefinition definition ;
	private final String filterExpr;
	private static final Logger logger = LoggerFactory.getLogger(NcdfEncoder.class);
	private final IAttributeValueParser attributeValueParser;
	private final int fetchSize;

	private IExpression selectionExpr;
	private String selectionSql;
	private ResultSet featureInstancesRS;

	public NcdfEncoder(
		IExprParser exprParser,
		IDialectTranslate translate,
		Connection conn,
		ICreateWritable createWritable,
		NcdfDefinition definition,
		String filterExpr
	) {
		this.exprParser = exprParser;
		this.translate = translate;
		this.conn = conn;
		this.createWritable = createWritable;
		this.definition = definition;
		this.filterExpr = filterExpr;
		this.attributeValueParser = new AttributeValueParser();  // TODO this class should not be responsible to instantiate

		fetchSize = 1000;
		featureInstancesRS = null;
		selectionExpr = null;
		selectionSql = null;
	}

	public void prepare() throws Exception
	{
		DataSource dataSource = definition.dataSource;

		// do not quote search path!.
		PreparedStatement s = conn.prepareStatement( "set search_path=" + dataSource.getSchema() + ", public");
		s.execute();
		s.close();

		selectionExpr = exprParser.parseExpression( filterExpr );
		selectionSql = translate.process( selectionExpr);

		// if we combine both tables, then it's actually simpler, since don't need to process twice
		// or discriminate about which attributes come from which tables.
		// And there's no optimisation penalty since both the initial and instance queries have to hit the big data table
		String query =
			"select distinct data.instance_id" +
			" from (" + dataSource.getVirtualDataTable() + ") as data" +
			" left join (" + dataSource.getVirtualInstanceTable() + ") instance" +
			" on instance.id = data.instance_id" +
			" where " + selectionSql + ";" ;


		PreparedStatement stmt = conn.prepareStatement( query );
		stmt.setFetchSize(fetchSize);

		// change name featureInstancesRSToProcess ?
		featureInstancesRS = stmt.executeQuery();

	}


	public InputStream get() throws Exception
	{
		try {
			if( featureInstancesRS.next())
			{
				long instanceId = featureInstancesRS.getLong(1); 

				String orderClause = "";
				for( IDimension dimension : definition.dimensions )
				{
					if( !orderClause.equals("")){
						orderClause += ",";
					}
					orderClause += "\"" + dimension.getName() + "\"" ;
				}

				DataSource dataSource = definition.dataSource;

				String query =
					"select *" +
					" from (" + dataSource.getVirtualDataTable() + ") as data" +
					" left join (" + dataSource.getVirtualInstanceTable() + ") instance" +
					" on instance.id = data.instance_id" +
					" where " + selectionSql +
					" and data.instance_id = " + Long.toString( instanceId) +
					" order by " + orderClause +
					";" ;

				logger.debug( "instanceId " + instanceId + ", " + query );

				populateValues( query, definition.dimensions, definition.variables );

				NetcdfFileWriteable writer = createWritable.create();

				// Write the global attributes
				for( Attribute attribute: definition.globalAttributes )
				{
					String name = attribute.getName();
					Object value = null;

					if( attribute.getValue() != null )
					{
						// convert to netcdf type
						AttributeValue convertedValue = attributeValueParser.parse( attribute.getValue() );
						value = convertedValue.value;
					}
					else if( attribute.getSql() != null )
					{
						// we need aliases for the inner select, and to support wrapping the where selection
						String sql = attribute.getSql().replaceAll( "\\$instance",
							"( select * " +
							" from (" + dataSource.getVirtualInstanceTable() + ") instance " +
							" where instance.id = " + Long.toString( instanceId) + ") as instance "
						);

						// as for vars/dims, but without the order clause, to support aggregate functions like min/max
						sql = sql.replaceAll( "\\$data",
							"( select *" +
							" from (" + dataSource.getVirtualDataTable() + ") as data" +
							" left join (" + dataSource.getVirtualInstanceTable() + ") instance" +
							" on instance.id = data.instance_id" +
							" where " + selectionSql +
							" and data.instance_id = " + Long.toString( instanceId) +
							" ) as data"
						);

						PreparedStatement stmt = null;
						try {
							stmt = conn.prepareStatement( sql );
							stmt.setFetchSize(fetchSize);
							ResultSet rs = stmt.executeQuery();

							// TODO more checks around this
							// maybe support converion to ncdf array attribute types
							rs.next();
							value = rs.getObject( 1 );
						} finally {
							stmt.close();
						}
					}
					else {
						throw new NcdfGeneratorException( "No value defined for global attribute '" + name + "'" );
					}

					if( value == null )
						logger.error( "Null found for attribute value '" + name + "'" );
					else if( value instanceof Number )
						writer.addGlobalAttribute( name, (Number) value );
					else if( value instanceof String )
						writer.addGlobalAttribute( name, (String) value );
					else if( value instanceof Array )
						writer.addGlobalAttribute( name, (Array) value );
					else
						throw new NcdfGeneratorException( "Unrecognized attribute type '" +  value.getClass().getName() + "'" );
				}

				// define dimensions
				for ( IDimension dimension: definition.dimensions) {
					dimension.define(writer);
				}

				// define vars
				for ( IVariable variable : definition.variables ) {
					variable.define( writer );
				}

				// finish netcdf definition
				writer.create();

				// write values
				for ( IVariable variable: definition.variables) {
					// maybe change name writeValues
					variable.finish( writer );
				}
				// finish the file
				writer.close();

				// return the stream
				return createWritable.getStream();
			}
			else {
				// no more netcdfs
				conn.close();
				return null;
			}
		}
		catch ( Exception e ) {
			logger.error( "Problem generating netcdf ", e );
			conn.close();
			// propagate to caller
			throw e;
		}
	}

	public void populateValues(
		String query,
		List< IDimension> dimensions,
		List< IVariable> encoders
		) throws Exception
	{
		// prepare buffers
		for( IDimension dimension : definition.dimensions )
			dimension.prepare();

		for( IVariable variable : definition.variables )
			variable.prepare();

		// sql stuff
		PreparedStatement stmt = conn.prepareStatement( query );
		stmt.setFetchSize(fetchSize);
		ResultSet rs = stmt.executeQuery();

		// now we loop the main attributes
		ResultSetMetaData m = rs.getMetaData();
		int numColumns = m.getColumnCount();


		// organize dimensions by name
		Map< String, IDimension> dimensionsMap = new HashMap< String, IDimension> ();
		for( IDimension dimension : dimensions )
			dimensionsMap.put( dimension.getName(), dimension );

		// organize variables by name
		Map< String, IVariable> encodersMap = new HashMap< String, IVariable> ();
		for( IVariable encoder : encoders )
			encodersMap.put( encoder.getName(), encoder );

		// pre-map the encoders by index according to the column name
		ArrayList< IAddValue> [] processing = (ArrayList< IAddValue> []) new ArrayList [numColumns + 1];

		for ( int i = 1 ; i <= numColumns ; i++ ) {

			processing[i] = new ArrayList< IAddValue> ();

			IDimension dimension = dimensionsMap.get( m.getColumnName(i));
			if( dimension != null)
				processing[i].add( dimension );

			IAddValue encoder = encodersMap.get(m.getColumnName(i));
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

}

