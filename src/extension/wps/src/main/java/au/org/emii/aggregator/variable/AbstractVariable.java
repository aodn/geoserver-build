package au.org.emii.aggregator.variable;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.FileWriter2.ChunkingIndex;

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
        private final long maxChunkElems;
        private final ChunkingIndex variableIndex;

        private int[] chunkShape;
        private Array values;
        private Index chunkIndex;

        NumericValueIterator(NetcdfVariable variable) {
            this.variable = variable;
            maxChunkElems = variable.getMaxChunkSize() / variable.getDataType().getSize();
            variableIndex = new ChunkingIndex(variable.getShape());
        }

        @Override
        public boolean hasNext() {
            return variableIndex.currentElement() < variableIndex.getSize();
        }

        @Override
        public NumericValue next() {
            try {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                if (values == null) {
                    int[] startingPosition = variableIndex.getCurrentCounter();
                    chunkShape = variableIndex.computeChunkShape(maxChunkElems);
                    values = variable.read(startingPosition, chunkShape);
                    chunkIndex = values.getIndex();
                }

                int[] index = getCurrentIndex(variableIndex, chunkIndex);
                Number value = (Number) values.getObject(chunkIndex);

                if (chunkIndex.currentElement() + 1 < values.getSize()) {
                    chunkIndex.incr();
                } else {
                    variableIndex.setCurrentCounter(variableIndex.currentElement() + (int) Index.computeSize(chunkShape));
                    values = null;
                }

                return new NumericValue(index, value);
            } catch (InvalidRangeException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        private int[] getCurrentIndex(Index variableIndex, Index chunkIndex) {
            int[] variableCounter = variableIndex.getCurrentCounter();
            int[] chunkCounter = chunkIndex.getCurrentCounter();
            int[] index = new int[variableIndex.getRank()];

            for (int i = 0; i < index.length; i++) {
                index[i] = variableCounter[i] + chunkCounter[i];
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