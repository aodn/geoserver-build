package au.org.emii.aggregator.coordsystem;

import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.Arrays;

import static ucar.unidata.geoloc.LatLonPointImpl.latNormal;
import static ucar.unidata.geoloc.LatLonPointImpl.lonNormal;

/**
 * Operations on latitude/longitude coordinates
 */
public class LatLonCoords {
    private final NetcdfVariable latitude;
    private final NetcdfVariable longitude;

    public LatLonCoords(NetcdfVariable latitude, NetcdfVariable longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LatLonRect getBbox() {
        if (latitude == null || longitude == null) {
            throw new UnsupportedOperationException("Dataset has no latitude/longitude axes");
        }

        return new LatLonRect(new LatLonPointImpl(latitude.getMin(), longitude.getMin()),
            new LatLonPointImpl(latitude.getMax(), longitude.getMax()));
    }

    public XYRanges getXYRanges(LatLonRect bbox) throws AggregationException {
        boolean pointSubset = bbox.getLowerLeftPoint().equals(bbox.getUpperRightPoint());

        if (longitude.getRank() == 1 && latitude.getRank() == 1) {
            if (pointSubset) {
                return getNearestXYPoint(longitude, latitude, bbox.getLonMin(), bbox.getLatMin());
            } else {
                return getXYRanges1D(bbox, longitude, latitude);
            }

        } else if (longitude.getRank() == 2 && latitude.getRank() == 2) {
            if (!Arrays.equals(longitude.getShape(), latitude.getShape())) {
                throw new UnsupportedOperationException(
                    "Longitude coordinate axis has different shape to latitude coordinate axis");
            }

            if (pointSubset) {
                return getNearestXYPoint2D(longitude, latitude, bbox.getLonMin(), bbox.getLatMin());
            } else {
                return getXYRanges2D(bbox, longitude, latitude);
            }
        } else {
            throw new UnsupportedOperationException("Longitude/Latitude must be either 1D or 2D only");
        }
    }

    public String getXDimensionName() {
        return longitude.getDimensions().get(0).getShortName();
    }

    public String getYDimensionName() {
        if (latitude.getRank() == 2) {
            return latitude.getDimensions().get(1).getShortName();
        } else {
            return latitude.getDimensions().get(0).getShortName();
        }
    }

    public class XYRanges {
        Range xRange;
        Range yRange;

        XYRanges(Range xRange, Range yRange) {
            this.xRange = xRange;
            this.yRange = yRange;
        }

        public Range getXRange() {
            return xRange;
        }

        public Range getYRange() {
            return yRange;
        }
    }

    private XYRanges getNearestXYPoint(NetcdfVariable lonAxis, NetcdfVariable latAxis, double longitude, double latitude) {
        try {
            int lastNearestX = -1; // last x index nearest to minimum lat/lon
            int lastNearestY = -1; // last y index nearest to minimum lat/lon

            double lastNearestDistance = Double.MAX_VALUE;

            Array lonValues = lonAxis.read();
            Array latValues = latAxis.read();

            for (int x=0; x<lonAxis.getShape()[0]; x++) {
                for (int y=0; y<latAxis.getShape()[0]; y++) {
                    double minLatLonDistance = distance(latValues.getDouble(y), latitude, lonValues.getDouble(x), longitude);

                    if (minLatLonDistance < lastNearestDistance) {
                        lastNearestX = x;
                        lastNearestY = y;
                        lastNearestDistance = minLatLonDistance;
                    }
                }
            }

            return new XYRanges(new Range(lastNearestX, lastNearestX), new Range(lastNearestY, lastNearestY));
        } catch (IOException|InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    private XYRanges getXYRanges2D(LatLonRect bbox, NetcdfVariable lonAxis2D, NetcdfVariable latAxis2D) throws AggregationException {
        try {
            int minX = Integer.MAX_VALUE; // minimum x index with lat/lon within bbox
            int minY = Integer.MAX_VALUE; // minimum y index with lat/lon within bbox
            int maxX = Integer.MIN_VALUE; // maximum x index with lat/lon within bbox
            int maxY = Integer.MIN_VALUE; // minimum y index with lat/lon within bbox

            Array lonValues = lonAxis2D.read();
            Array latValues = latAxis2D.read();

            for (int x=0; x<lonAxis2D.getShape()[0]; x++) {
                for (int y = 0; y < lonAxis2D.getShape()[1]; y++) {
                    double longitude = lonValues.getDouble(x * lonValues.getShape()[1] + y);
                    double latitude = latValues.getDouble(x * latValues.getShape()[1] + y);

                    if (bbox.contains(new LatLonPointImpl(latitude, longitude))) {
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (minX == Integer.MAX_VALUE) {
                throw new AggregationException("Bounding box selected no data");
            }

            return new XYRanges(new Range(minX, maxX), new Range(minY, maxY));
        } catch (IOException|InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    private XYRanges getNearestXYPoint2D(NetcdfVariable lonAxis2D, NetcdfVariable latAxis2D,
                                        double longitude, double latitude) {
        try {
            int lastNearestX = -1; // last x index nearest to minimum lat/lon
            int lastNearestY = -1; // last y index nearest to minimum lat/lon

            double lastNearestDistance = Double.MAX_VALUE;

            Array lonValues = lonAxis2D.read();
            Array latValues = latAxis2D.read();

            for (int x=0; x<lonAxis2D.getShape()[0]; x++) {
                for (int y = 0; y < lonAxis2D.getShape()[1]; y++) {
                    double lonValue = lonValues.getDouble(x * lonValues.getShape()[1] + y);
                    double latValue = latValues.getDouble(x * latValues.getShape()[1] + y);

                    double minLatLonDistance = distance(latValue, latitude,
                        lonValue, longitude);

                    if (minLatLonDistance < lastNearestDistance) {
                        lastNearestX = x;
                        lastNearestY = y;
                        lastNearestDistance = minLatLonDistance;
                    }
                }
            }

            return new XYRanges(new Range(lastNearestX, lastNearestX), new Range(lastNearestY, lastNearestY));
        } catch (IOException|InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    private XYRanges getXYRanges1D(LatLonRect bbox, NetcdfVariable lonAxis1D, NetcdfVariable latAxis1D) throws AggregationException {
        try {
            Array lonValues = lonAxis1D.read();
            Array latValues = latAxis1D.read();

            CoordRange longitudeRange = new LongitudeRange(bbox.getLonMin(), bbox.getLonMax());
            CoordRange latitudeRange = new LatitudeRange(bbox.getLatMin(), bbox.getLatMax());

            Range xRange = getIndexRange(lonValues, longitudeRange);
            Range yRange = getIndexRange(latValues, latitudeRange);

            if (xRange == null || yRange == null) {
                throw new AggregationException("Bounding box selected no data");
            }

            return new XYRanges(xRange, yRange);
        } catch (IOException|InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    private Range getIndexRange(Array values, CoordRange range) throws InvalidRangeException {
        int minIdx = Integer.MAX_VALUE; // minimum index within coordinate range
        int maxIdx = Integer.MIN_VALUE; // maximum index within coordinate range

        for (int idx=0; idx<values.getSize(); idx++) {
            double value = values.getDouble(idx);

            if (range.contains(value)) {
                if (idx < minIdx) minIdx = idx;
                if (idx > maxIdx) maxIdx = idx;
            }
        }

        return minIdx == Integer.MAX_VALUE ? null : new Range(minIdx, maxIdx);
    }

    /**
     * From: http://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
     * Calculate distance between two points in latitude and longitude.
     * Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point
     * @returns Distance in Meters
     */
    private double distance(double lat1, double lat2, double lon1,
                                  double lon2) {
        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }
}
