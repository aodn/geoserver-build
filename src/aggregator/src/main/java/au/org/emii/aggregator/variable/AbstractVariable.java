package au.org.emii.aggregator.variable;

import au.org.emii.aggregator.index.IndexChunkIterator;
import au.org.emii.aggregator.index.IndexChunk;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Common code for implementations of NetcdfVariable
 */
public abstract class AbstractVariable implements NetcdfVariable {
    final long maxChunkSize;

    private Bounds bounds;

    protected AbstractVariable() {
        this.maxChunkSize = DEFAULT_MAX_CHUNK_SIZE;
    }

    protected AbstractVariable(long maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    public long getSize() {
        long size = 0;

        for (Dimension dimension : getDimensions()) {
            size += dimension.getLength();
        }

        return size;
    }

    @Override
    public int[] getShape() {
        List<Dimension> dimensions = getDimensions();

        int[] result = new int[dimensions.size()];

        for (int i = 0; i < dimensions.size(); i++) {
            result[i] = dimensions.get(i).getLength();
        }

        return result;
    }

    @Override
    public int getRank() {
        return getShape().length;
    }

    @Override
    public Attribute findAttribute(String attName) {
        for (Attribute attribute : getAttributes()) {
            if (attribute.getShortName().equals(attName)) {
                return attribute;
            }
        }

        return null;
    }

    @Override
    public Array read() throws IOException {
        try {
            int[] origin = new int[getRank()];
            return read(origin, getShape());
        } catch (InvalidRangeException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    public boolean isUnlimited() {
        for (Dimension dimension : getDimensions()) {
            if (dimension.isUnlimited()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterable<NumericValue> getNumericValues() {
        if (!getDataType().isNumeric()) {
            throw new UnsupportedOperationException("Not a numeric data type");
        }

        return new Iterable<NumericValue>() {
            @Override
            public Iterator<NumericValue> iterator() {
                return new NumericValueIterator(AbstractVariable.this);
            }
        };
    }


    @Override
    public Bounds getBounds() {
        if (bounds == null) {
            bounds = calculateBounds();
        }

        return bounds;
    }

    @Override
    public long getMaxChunkSize() {
        return maxChunkSize;
    }

    private Bounds calculateBounds() {
        double min = Double.MAX_VALUE;
        double max = Double.MAX_VALUE * -1;

        for (NumericValue value: getNumericValues()) {
            if (value.getValue().doubleValue() < min) min = value.getValue().doubleValue();
            if (value.getValue().doubleValue() > max) max = value.getValue().doubleValue();
        }

        return new Bounds(min, max);
    }


    private class NumericValueIterator implements Iterator<NumericValue> {

        private final NetcdfVariable variable;
        private final IndexChunkIterator variableIndex;

        private Array values;
        private Index chunkIndex;
        private IndexChunk currentChunk;

        NumericValueIterator(NetcdfVariable variable) {
            this.variable = variable;
            long maxChunkElems = variable.getMaxChunkSize() / variable.getDataType().getSize();
            variableIndex = new IndexChunkIterator(variable.getShape(), maxChunkElems);
        }

        @Override
        public boolean hasNext() {
            return variableIndex.hasNext() || values != null;
        }

        @Override
        public NumericValue next() {
            try {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                if (values == null) {
                    currentChunk = variableIndex.next();
                    values = variable.read(currentChunk.getOffset(), currentChunk.getShape());
                    chunkIndex = values.getIndex();
                }

                int[] index = getCurrentIndex(currentChunk, chunkIndex);
                Number value = (Number) values.getObject(chunkIndex);

                if (chunkIndex.currentElement() + 1 < values.getSize()) {
                    chunkIndex.incr();
                } else {
                    values = null;
                }

                return new NumericValue(index, value);
            } catch (InvalidRangeException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        private int[] getCurrentIndex(IndexChunk chunk, Index chunkIndex) {
            int[] index = chunk.getOffset();
            int[] chunkPosition = chunkIndex.getCurrentCounter();

            for (int i = 0; i < index.length; i++) {
                index[i] += chunkPosition[i];
            }

            return index;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }

    }

    public class NumericValue {
        private int[] index;
        private Number value;

        NumericValue(int[] index, Number value) {
            this.index = index;
            this.value = value;
        }

        public int[] getIndex() {
            return index;
        }

        public Number getValue() {
            return value;
        }
    }
}