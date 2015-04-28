
package au.org.emii.ncdfgenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import ucar.nc2.NetcdfFileWriteable;
import ucar.ma2.Array;
import ucar.nc2.Dimension;


class Variable implements IVariable {
    private final String variableName;
    private final List<IDimension> dimensions;
    private final IValueEncoder encodeValue;
    private final List<Attribute> attributes;
    private final IAttributeValueParser attributeValueParser;
    private final List< Object > convertedAttributes; // output ordered (change name Values  )
    private final Map< String, Object > convertedAttributesMap; // to support encoder lookup...
    private List<Object> buffer;

    public Variable(
        String variableName,
        List< IDimension> dimensions,
        IValueEncoder encodeValue,
        List<Attribute> attributes
    ) {
        this.variableName = variableName;
        this.encodeValue = encodeValue;
        this.attributes = attributes;
        this.dimensions = dimensions;
        this.attributeValueParser = new AttributeValueParser();  // TODO this class should not be responsible to instantiate
        this.convertedAttributes = new ArrayList< Object >();
        this.convertedAttributesMap = new HashMap< String, Object >();
        this.buffer = null;
    }

    public void prepare() {
        buffer = new ArrayList<Object>();
    }

    public void addValueToBuffer(Object value) {
        // perhaps delegate to strategy...
        buffer.add(value);
    }


    public final void define(NetcdfFileWriteable writer) throws NcdfGeneratorException {
        // write dims and attributes

        // make sure children are defined already
        List<Dimension> d = new ArrayList<Dimension>();
        for(IDimension dimension: dimensions) {
            d.add(dimension.getDimension());
        }

        writer.addVariable(variableName, encodeValue.targetType(), d);

        // there's a bit of double handling here. We use the list to preserve output ordering
        // but use a map for the encoder type to permit easy name lookup
        for(Attribute a : attributes) {
            AttributeValue av = attributeValueParser.parse(a.getValue());
            convertedAttributes.add(av.getValue());
            convertedAttributesMap.put(a.getName(), av.getValue());
        }

        // encode the variable attributes
        for(int i = 0; i < attributes.size(); ++i) {

            // https://www.unidata.ucar.edu/software/thredds/v4.3/netcdf-java/v4.2/javadoc/ucar/nc2/NetcdfFileWriteable.html
            String name = attributes.get(i).getName();
            Object value = convertedAttributes.get(i);

            if(value instanceof Number) {
                writer.addVariableAttribute(variableName, name, (Number) value);
            } else if(value instanceof String) {
                writer.addVariableAttribute(variableName, name, (String) value);
            } else if(value instanceof Array) {
                writer.addVariableAttribute(variableName, name, (Array) value);
            } else {
                throw new NcdfGeneratorException("Unrecognized attribute type '" +  value.getClass().getName() + "'");
            }
        }
    }


    public void writeValues(List<IDimension> dims, int dimIndex, int acc, Array array)
    throws NcdfGeneratorException {
        if(dimIndex < dims.size()) {
            Dimension dim = dims.get(dimIndex).getDimension();
            for(int i = 0; i < dim.getLength(); i++) {
                writeValues(dims, dimIndex + 1, acc + i, array);
            }
        } else {
            encodeValue.encode(array, acc, buffer.get(acc));
        }
    }


    private static int[] toIntArray(List<Integer> list) {
        // List.toArray() only supports java Boxed Integers...
        int[] ret = new int[list.size()];
        for(int i = 0; i < ret.length; i++)
            ret[i] = list.get(i);
        return ret;
    }


    public void finish(NetcdfFileWriteable writer) throws Exception { // TODO use narrow exception
        ArrayList< Integer> shape = new ArrayList< Integer>();
        for(IDimension dimension : dimensions) {
            shape.add(dimension.getLength());
        }

        Array array = Array.factory(encodeValue.targetType(), toIntArray(shape));

        encodeValue.prepare(convertedAttributesMap);

        if(buffer.isEmpty()) {
            throw new NcdfGeneratorException("No values found for variable '" + variableName + "'");
        }

        writeValues(dimensions,  0, 0 , array);

        // int [] origin = new int[1];
        // writer.write(variableName, origin, A);
        writer.write(variableName, array);
    }


    public String getName() {
        return variableName;
    }
}

