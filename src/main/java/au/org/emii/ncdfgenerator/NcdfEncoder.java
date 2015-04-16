
package au.org.emii.ncdfgenerator;

import au.org.emii.ncdfgenerator.cql.IExpression;
import au.org.emii.ncdfgenerator.cql.IExprParser;
import au.org.emii.ncdfgenerator.cql.IDialectTranslate;


import java.util.Map;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

import org.w3c.dom.Document;

import ucar.nc2.NetcdfFileWriteable;


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
		selection_expr = exprParser.parseExpression( filterExpr );
		// bad, should return expr or throw
		if(selection_expr == null) {
			throw new NcdfGeneratorException( "failed to parse expression" );
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
					throw new NcdfGeneratorException( "Can't convert intance_id type to integer" );
				}

				System.out.println( "instance_id is " + instance_id );

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


