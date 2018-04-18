package au.org.emii.ncdfgenerator;

import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Variable implements IVariable {
    private final String variableName;
    private final List<IDimension> dimensions;
    private final IValueEncoder encodeValue;
    private final List<Attribute> attributes;
    private final IAttributeValueParser attributeValueParser;
    private List<Object> convertedAttributes; // output ordered (change name Values  )
    private Map<String, Object> convertedAttributesMap; // to support encoder lookup...
    private List<Object> buffer;
    private ucar.nc2.Variable variable;
    private int[] origin;

    public Variable(
        String variableName,
        List<IDimension> dimensions,
        IValueEncoder encodeValue,
        List<Attribute> attributes
    ) {
        this.variableName = variableName;
        this.encodeValue = encodeValue;
        this.attributes = attributes;
        this.dimensions = dimensions;
        this.attributeValueParser = new AttributeValueParser();  // TODO this class should not be instantiated here
        this.convertedAttributes = null;
        this.convertedAttributesMap = null;
        this.buffer = null;
    }

    public void prepare() {
        convertedAttributes = new ArrayList<Object>();
        convertedAttributesMap = new HashMap<String, Object>();
        buffer = new ArrayList<Object>();
        origin = new int[dimensions.size()]; // buffer position within variable
    }

    public void addValueToBuffer(Object value) {
        // perhaps delegate to strategy...
        buffer.add(value);
    }

    public final void define(NetcdfFileWriter writer) throws NcdfGeneratorException {
        // write dims and attributes

        // make sure children are defined already
        List<Dimension> d = new ArrayList<Dimension>();
        for (IDimension dimension : dimensions) {
            d.add(dimension.getDimension());
        }

        variable = writer.addVariable(null, variableName, encodeValue.targetType(), d);

        // there's a bit of double handling here. We use the list to preserve output ordering
        // but use a map for the encoder type to permit easy name lookup
        for (Attribute a : attributes) {
            AttributeValue av = attributeValueParser.parse(a.getValue());
            convertedAttributes.add(av.getValue());
            convertedAttributesMap.put(a.getName(), av.getValue());
        }

        // encode the variable attributes
        for (int i = 0; i < attributes.size(); ++i) {

            // https://www.unidata.ucar.edu/software/thredds/v4.3/netcdf-java/v4.2/javadoc/ucar/nc2/NetcdfFileWriteable.html
            String name = attributes.get(i).getName();
            Object value = convertedAttributes.get(i);

            if (value instanceof Number) {
                writer.addVariableAttribute(variable, new ucar.nc2.Attribute(name, (Number)value));
            }
            else if (value instanceof String) {
                writer.addVariableAttribute(variable, new ucar.nc2.Attribute(name, (String)value));
            }
            else if (value instanceof Array) {
                writer.addVariableAttribute(variable, new ucar.nc2.Attribute(name, (Array)value));
            }
            else {
                throw new NcdfGeneratorException("Unrecognized attribute type '" + value.getClass().getName() + "'");
            }
        }

        encodeValue.prepare(convertedAttributesMap);
    }

    public void encodeValues(int[] shape, int index, int acc, Array array)
        throws NcdfGeneratorException {
        if (index < shape.length) {
            for (int i = 0; i < shape[index]; i++) {
                encodeValues(shape, index + 1, acc + i, array);
            }
        }
        else {
            encodeValue.encode(array, acc, buffer.get(acc));
        }
    }

    public void flushBuffer(NetcdfFileWriter writer) throws Exception { // TODO use narrow exception
        int[] shape = new int[dimensions.size()];

        int recordSize = 1;

        for (int i=dimensions.size()-1; i >= 0; i--) {
            IDimension dimension = dimensions.get(i);

            if (dimension.isUnlimited()) {
                shape[i] = buffer.size() / recordSize;
            } else {
                shape[i] = dimension.getLength();
                recordSize *= shape[i];
            }
        }

        Array array = Array.factory(encodeValue.targetType(), shape);

        if (buffer.isEmpty()) {
            throw new NcdfGeneratorException("No values found for variable '" + variableName + "'");
        }

        encodeValues(shape, 0, 0, array);

        writer.write(variable, origin, array);

        if (origin.length > 0) {
            origin[0] += buffer.size() / recordSize;
        }

        buffer.clear();
    }

    public String getName() {
        return variableName;
    }
}

