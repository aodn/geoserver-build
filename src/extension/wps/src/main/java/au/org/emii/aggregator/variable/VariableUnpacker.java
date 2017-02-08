package au.org.emii.aggregator.variable;

import au.org.emii.aggregator.variable.datatype.NumericTypes;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

import java.io.IOException;

/**
 * Class that can be used to unpack a variable/change its type
 *
 * Allows default filler value, missing values, valid range, valid min and valid max values to be used in the
 * converted variable to be overridden as required.
 */

public class VariableUnpacker {

    private final Variable variable;
    private final Number scale;
    private final Number offset;
    private final Number[] newValidRange;
    private final Number newValidMin;
    private final Number newValidMax;
    private final Number oldFillerValue;
    private final Number[] oldMissingValues;
    private final Number newFillerValue;
    private final Number[] newMissingValues;
    private final DataType newDataType;
    private final boolean conversionRequired;
    private final boolean isUnsigned;

    public VariableUnpacker(Variable variable) {
        this(variable, new UnpackerOverrides.Builder().build()); // use defaults - no overrides
    }

    public VariableUnpacker(Variable variable, UnpackerOverrides overrides) {
        this.variable = variable;

        // get scale factor to use

        Attribute scaleAttribute = variable.findAttribute(CDM.SCALE_FACTOR);

        if (scaleAttribute != null && !scaleAttribute.isString()) {
            scale = scaleAttribute.getNumericValue();
        } else {
            scale = null;
        }

        // get offset to add

        Attribute offsetAttribute = variable.findAttribute(CDM.ADD_OFFSET);

        if (offsetAttribute != null && !offsetAttribute.isString()) {
            offset = offsetAttribute.getNumericValue();
        } else {
            offset = null;
        }

        // store for performance reasons

        isUnsigned = variable.isUnsigned();  // applyScaleOffset is 10 times slower otherwise

        // get data type to use

        if (overrides.getNewDataType() != null) {
            newDataType = overrides.getNewDataType();
        } else if (scale != null) {
            newDataType = DataType.getType(scale.getClass());
        } else if (offset != null) {
            newDataType = DataType.getType(offset.getClass());
        } else {
            newDataType = variable.getDataType();
        }

        // get valid range to use

        Attribute validRangeAtt = variable.findAttribute(CDM.VALID_RANGE);

        if (overrides.getNewValidRange() != null) {
            newValidRange = new Number[2];
            newValidRange[0] = NumericTypes.valueOf(overrides.getNewValidRange()[0], newDataType);
            newValidRange[1] = NumericTypes.valueOf(overrides.getNewValidRange()[1], newDataType);
        } else if (validRangeAtt == null || validRangeAtt.isString() || validRangeAtt.getLength() < 2) {
            newValidRange = null;
        } else {
            newValidRange = new Number[2];
            newValidRange[0] = applyScaleOffset(validRangeAtt.getNumericValue(0));
            newValidRange[1] = applyScaleOffset(validRangeAtt.getNumericValue(1));
        }

        // get valid min to use

        Attribute validMinAttribute = variable.findAttribute("valid_min");

        if (newValidRange != null) {
            newValidMin = null;
        } else if (overrides.getNewValidMin() != null) {
            newValidMin = NumericTypes.valueOf(overrides.getNewValidMin(), newDataType);
        } else if (validMinAttribute == null || validMinAttribute.isString()) {
            newValidMin = null;
        } else {
            newValidMin = applyScaleOffset(validMinAttribute.getNumericValue());
        }

        // get valid max to use

        Attribute validMaxAttribute = variable.findAttribute("valid_max");

        if (newValidRange != null) {
            newValidMax = null;
        } else if (overrides.getNewValidMax() != null) {
            newValidMax = NumericTypes.valueOf(overrides.getNewValidMax(), newDataType);
        } else if (validMaxAttribute == null || validMaxAttribute.isString()) {
            newValidMax = null;
        } else {
            newValidMax = applyScaleOffset(validMaxAttribute.getNumericValue());
        }

        // get old fill value used

        Attribute fillValueAttribute = variable.findAttribute(CDM.FILL_VALUE);

        if (fillValueAttribute == null || fillValueAttribute.isString()) {
            oldFillerValue = null;
        } else {
            oldFillerValue = fillValueAttribute.getNumericValue();
        }

        // get new fill value to use

        if (oldFillerValue == null) {
            newFillerValue = null;
        } else if (overrides.getNewFillerValue() != null) {
            newFillerValue = NumericTypes.valueOf(overrides.getNewFillerValue(), newDataType);
        } else if (NumericTypes.isDefaultFillValue(oldFillerValue, variable.getDataType())) {
                newFillerValue = NumericTypes.defaultFillValue(newDataType);
        } else {
            newFillerValue = applyScaleOffset(oldFillerValue);
        }

        // get old missing value used

        Attribute missingValueAttribute = variable.findAttribute(CDM.MISSING_VALUE);

        if (missingValueAttribute == null || missingValueAttribute.isString()) {
            oldMissingValues = null;
        } else {
            Class classType = missingValueAttribute.getDataType().getClassType();
            oldMissingValues = (Number[]) missingValueAttribute.getValues().get1DJavaArray(classType);
        }

        // get new missing value to use

        if (oldMissingValues == null) {
            newMissingValues = null;
        } else if (overrides.getNewMissingValues() != null) {
            newMissingValues = NumericTypes.valueOf(overrides.getNewMissingValues(), newDataType);
        } else {
            newMissingValues = applyScaleOffset(oldMissingValues);
        }

        // do we need to convert individual values?

        conversionRequired = scale != null || offset != null || newFillerValue != null || newMissingValues != null
            || newDataType != null;
    }

    public Array read(int[] origin, int[] slice) throws IOException, InvalidRangeException {
        Array data = variable.read(origin, slice);

        if (conversionRequired) {
            return convert(data);
        } else {
            return data;
        }
    }

    // Access to derived metadata for testing purposes

    public Number getFillerValue() {
        return newFillerValue;
    }

    public Number getValidMin() {
        return newValidMin;
    }

    public Number getValidMax() {
        return newValidMax;
    }

    public Number[] getValidRange() {
        return newValidRange;
    }

    public Number[] getMissingValues() {
        return newMissingValues;
    }

    // Private methods

    private Number[] applyScaleOffset(Number[] oldMissingValues) {
        Number[] result = new Number[oldMissingValues.length];

        for (int i=0; i <oldMissingValues.length; i++) {
            result[i] = applyScaleOffset(oldMissingValues[i]);
        }

        return result;
    }

    private Array convert(Array data) {
        Array result = Array.factory(newDataType, data.getShape());

        for (int i=0; i<data.getSize(); i++) {
            final Number value = (Number) data.getObject(i);
            result.setObject(i, convert(value));
        }

        return result;
    }

    private Number convert(Number value) {
        if (oldFillerValue != null && value.equals(oldFillerValue)) {
            return newFillerValue;
        } else if (oldMissingValues != null && isMissingValue(value)) {
            return newMissingValue(value);
        } else {
            return applyScaleOffset(value);
        }
    }

    private Number newMissingValue(Number value) {
        for (int i=0; i<oldMissingValues.length && i<newMissingValues.length; i++) {
            if (value.equals(oldMissingValues[i])) {
                return newMissingValues[i];
            }
        }

        return newMissingValues[0];
    }

    private boolean isMissingValue(Number value) {
        for (int i=0; i< oldMissingValues.length; i++) {
            if (value.equals(oldMissingValues[i])) {
                return true;
            }
        }

        return false;
    }

    private Number applyScaleOffset(Number value) {
        Number result = value;

        if (scale != null || offset != null) {
            double effectiveScale = scale != null ? scale.doubleValue() : 1.0;
            double effectiveOffset = offset != null ? offset.doubleValue() : 0.0;

            if (isUnsigned && value instanceof Byte)
                result = effectiveScale * DataType.unsignedByteToShort((Byte)value) + effectiveOffset;
            else if (isUnsigned && value instanceof Short)
                result = effectiveScale * DataType.unsignedShortToInt((Short)value) + effectiveOffset;
            else if (isUnsigned && value instanceof Integer)
                result = effectiveScale * DataType.unsignedIntToLong((Integer)value) + effectiveOffset;
            else {
                result = effectiveScale * value.doubleValue() + effectiveOffset;
            }
        }

        return NumericTypes.valueOf(result, newDataType);
    }

}
