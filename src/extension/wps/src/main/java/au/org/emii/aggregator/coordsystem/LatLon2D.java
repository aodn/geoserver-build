package au.org.emii.aggregator.coordsystem;

import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.FileWriter2.ChunkingIndex;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/* Dependent 2 dimensional latitude/longitude subset operations */

class LatLon2D extends LatLonCoords {
    LatLon2D(NetcdfVariable latitude, NetcdfVariable longitude) {
        super(latitude, longitude);
    }

    @Override
    public XYRanges getNearestXYPoint(double longitudeValue, double latitudeValue) throws AggregationException {
        try {
            int lastNearestX = -1; // last x index nearest to minimum lat/lon
            int lastNearestY = -1; // last y index nearest to minimum lat/lon

            double lastNearestDistance = Double.MAX_VALUE;

            for (LatLonValue latLonValue: getLatLonValues()) {
                double lonValue = latLonValue.getLon();
                double latValue = latLonValue.getLat();

                double minLatLonDistance = distance(latValue, latitudeValue,
                    lonValue, longitudeValue);

                if (minLatLonDistance < lastNearestDistance) {
                    lastNearestX = latLonValue.getX();
                    lastNearestY = latLonValue.getY();
                    lastNearestDistance = minLatLonDistance;
                }
            }

            return new XYRanges(new Range(lastNearestX, lastNearestX), new Range(lastNearestY, lastNearestY));
        } catch (InvalidRangeException e) {
            throw new AggregationException(e);
        }
    }

    @Override
    public XYRanges subsetGrid(LatLonRect bboxRequested) throws AggregationException {
        try {
            int minX = Integer.MAX_VALUE; // minimum x index with lat/lon within bbox
            int minY = Integer.MAX_VALUE; // minimum y index with lat/lon within bbox
            int maxX = Integer.MIN_VALUE; // maximum x index with lat/lon within bbox
            int maxY = Integer.MIN_VALUE; // minimum y index with lat/lon within bbox

            for (LatLonValue latLonValue: getLatLonValues()) {
                if (bboxRequested.contains(latLonValue.getLatLonPoint())) {
                    if (latLonValue.getX() < minX) minX = latLonValue.getX();
                    if (latLonValue.getY() < minY) minY = latLonValue.getY();
                    if (latLonValue.getX() > maxX) maxX = latLonValue.getX();
                    if (latLonValue.getY() > maxY) maxY = latLonValue.getY();
                }
            }

            if (minX == Integer.MAX_VALUE) {
                throw new AggregationException("Bounding box selected no data");
            }

            return new XYRanges(new Range(minX, maxX), new Range(minY, maxY));
        } catch (InvalidRangeException e) {
            throw new AggregationException(e);
        }
    }

    Iterable<LatLonValue> getLatLonValues() {
        return new Iterable<LatLonValue>() {
            @Override
            public Iterator<LatLonValue> iterator() {
                return new LatLonValue2DIterator();
            }
        };
    }

    /* Iterator which will iterate over the x/y, lat/lon values reading only up */
    /* to maxChunkSize elements into memory at a time */

    private class LatLonValue2DIterator implements Iterator<LatLonValue> {

        private final long maxChunkElems;
        private final ChunkingIndex variableIndex;

        private int[] chunkShape;
        private Array latChunkValues;
        private Array lonChunkValues;
        private Index chunkIndex;

        LatLonValue2DIterator() {
            long maxChunkSize = latitude.getMaxChunkSize() > longitude.getMaxChunkSize() ? longitude.getMaxChunkSize() :
                latitude.getMaxChunkSize();

            maxChunkElems = maxChunkSize / latitude.getDataType().getSize();

            variableIndex = new ChunkingIndex(latitude.getShape());
        }

        @Override
        public boolean hasNext() {
            return variableIndex.currentElement() < variableIndex.getSize();
        }

        @Override
        public LatLonValue next() {
            try {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                if (latChunkValues == null) {
                    int[] startingPosition = variableIndex.getCurrentCounter();
                    chunkShape = variableIndex.computeChunkShape(maxChunkElems);
                    latChunkValues = latitude.read(startingPosition, chunkShape);
                    lonChunkValues = longitude.read(startingPosition, chunkShape);
                    chunkIndex = latChunkValues.getIndex();
                }

                int currentX = variableIndex.getCurrentCounter()[0] + chunkIndex.getCurrentCounter()[0];
                int currentY = variableIndex.getCurrentCounter()[1] + chunkIndex.getCurrentCounter()[1];

                double latitude = latChunkValues.getDouble(chunkIndex);
                double longitude = lonChunkValues.getDouble(chunkIndex);

                if (chunkIndex.currentElement() + 1 < latChunkValues.getSize()) {
                    chunkIndex.incr();
                } else {
                    variableIndex.setCurrentCounter(variableIndex.currentElement() + (int) Index.computeSize(chunkShape));
                    latChunkValues = null;
                    lonChunkValues = null;
                }

                return new LatLonValue(currentX, currentY, latitude, longitude);
            } catch (InvalidRangeException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }

    }

    class LatLonValue {
        private final int x;
        private final int y;
        private final double lat;
        private final double lon;

        LatLonValue(int x, int y, double lat, double lon) {
            this.x = x;
            this.y = y;
            this.lat = lat;
            this.lon = lon;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        LatLonPoint getLatLonPoint() {
            return new LatLonPointImpl(lat, lon);
        }
    }

}
