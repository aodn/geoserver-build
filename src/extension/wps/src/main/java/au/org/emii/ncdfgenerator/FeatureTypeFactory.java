package au.org.emii.ncdfgenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.sql.Connection;
import java.sql.SQLException;

import org.geotools.jdbc.VirtualTable;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.type.*;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class FeatureTypeFactory {

    public static SimpleFeatureType getFeatureType(JDBCDataStore store, String virtualTableDef, boolean guessGeometrySrid) throws IOException {
        String vtName = null;

        try {
            vtName = UUID.randomUUID().toString();

            VirtualTable vt = new VirtualTable(vtName, virtualTableDef);
            store.createVirtualTable(vt);

            return guessFeatureType(store, vt.getName(), guessGeometrySrid);
        }
        finally {
            store.dropVirtualTable(vtName);
        }
    }

    // Lifted from: https://github.com/geoserver/geoserver/blob/master/src/web/core/src/main/java/org/geoserver/web/data/layer/SQLViewAbstractPage.java#L404
    private static SimpleFeatureType guessFeatureType(JDBCDataStore store, String vtName, boolean guessGeometrySrid) throws IOException {
        SimpleFeatureType base = store.getSchema(vtName);
        List<String> geometries = new ArrayList<String>();
        for (AttributeDescriptor ad : base.getAttributeDescriptors()) {
            if (ad instanceof GeometryDescriptor) {
                geometries.add(ad.getLocalName());
            }
        }

        // no geometries? Or, shall we not try to guess the geometries type and srid?
        if (geometries.size() == 0 || !guessGeometrySrid) {
            return base;
        }

        // build a query to fetch the first rwo, we'll inspect the resulting
        // geometries
        Query q = new Query(vtName);
        q.setPropertyNames(geometries);
        q.setMaxFeatures(1);
        SimpleFeatureIterator it = null;
        SimpleFeature f = null;
        try {
            it = store.getFeatureSource(vtName).getFeatures(q).features();
            if (it.hasNext()) {
                f = it.next();
            }
        } finally {
            if (it != null) {
                it.close();
            }
        }

        // did we get more information?
        if (f == null) {
            return base;
        }

        // if so, try to build an override feature type
        Connection cx = null;
        try {
            cx = store.getConnection(Transaction.AUTO_COMMIT);
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setName(base.getName());
            for (AttributeDescriptor ad : base.getAttributeDescriptors()) {
                if (ad instanceof GeometryDescriptor) {
                    GeometryDescriptor gd = (GeometryDescriptor) ad;
                    Geometry g = (Geometry) f.getAttribute(ad.getLocalName());
                    if (g == null) {
                        // nothing new we can learn
                        tb.add(ad);
                    } else {
                        Class binding = g.getClass();
                        CoordinateReferenceSystem crs = null;
                        if (g.getSRID() > 0) {
                            // see if the dialect can handle this one
                            crs = store.getSQLDialect().createCRS(g.getSRID(), cx);
                            tb.userData(JDBCDataStore.JDBC_NATIVE_SRID, g.getSRID());
                        }
                        if (crs == null) {
                            crs = gd.getCoordinateReferenceSystem();
                        }
                        tb.add(ad.getLocalName(), binding, crs);
                    }

                } else {
                    tb.add(ad);
                }
            }
            return tb.buildFeatureType();
        } catch (SQLException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } finally {
            store.closeSafe(cx);
        }
    }
}
