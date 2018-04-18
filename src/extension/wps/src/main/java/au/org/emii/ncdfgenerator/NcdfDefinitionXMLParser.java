package au.org.emii.ncdfgenerator;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

final class Helper {

    private Helper() {
        // not called
    }

    static String nodeVal(Node node) {
        Node child = node.getFirstChild();
        if (child != null && child.getNodeType() == Node.TEXT_NODE) {
            return child.getNodeValue();
        }
        return "";
    }

    static boolean isElementNode(Node node) {
        return node.getNodeType() == Node.ELEMENT_NODE;
    }
}

public class NcdfDefinitionXMLParser {
    class Context {
        // required to resolve references to dimensions from the variable definitions
        private List<IDimension> dimensions;

        IDimension getDimensionByName(String name) {
            for (IDimension dimension : dimensions) {
                if (dimension.getName().equals(name)) {
                    return dimension;
                }
            }
            return null;
        }

        void setDimensions(List<IDimension> dimensions) {
            this.dimensions = dimensions;
        }
    }

    public NcdfDefinition parse(Node node) throws NcdfGeneratorException {
        if (node.getNodeName().equals("definition")) {
            return new DefinitionParser().parse(node);
        }
        else {
            throw new NcdfGeneratorException("Missing definition");
        }
    }

    class DefinitionParser {
        NcdfDefinition parse(Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("definition")) {
                throw new NcdfGeneratorException("Not definition");
            }

            DataSource dataSource = null;
            FilenameTemplate filenameTemplate = null;
            List<IVariable> variables = null;
            List<Attribute> globalAttributes = null;

            Context context = new Context();

            for (Node child : new NodeWrapper(node)) {
                if (Helper.isElementNode(child)) {
                    String tag = child.getNodeName();
                    if (tag.equals("source")) {
                        dataSource = new DataSourceParser().parse(child);
                    }
                    else if (tag.equals("filename")) {
                        filenameTemplate = new FilenameTemplateParser().parse(child);
                    }
                    else if (tag.equals("dimensions")) {
                        context.setDimensions(new DimensionsParser().parse(child));
                    }
                    else if (tag.equals("variables")) {
                        variables = new VariableParsers().parse(context, child);
                    }
                    else if (tag.equals("globalattributes")) {
                        globalAttributes = new AttributesParser().parse(child);
                    }
                    else {
                        throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
                    }
                }
            }

            return new NcdfDefinition(dataSource, filenameTemplate, globalAttributes, context.dimensions, variables);
        }
    }

    class DataSourceParser {
        private String dataStoreName;
        private String virtualDataTable;
        private String virtualInstanceTable;

        private void extractValue(Node child) throws NcdfGeneratorException {
            String tag = child.getNodeName();
            if (tag.equals("dataStoreName")) {
                dataStoreName = Helper.nodeVal(child);
            }
            else if (tag.equals("virtualDataTable")) {
                virtualDataTable = Helper.nodeVal(child);
            }
            else if (tag.equals("virtualInstanceTable")) {
                virtualInstanceTable = Helper.nodeVal(child);
            }
            else {
                throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
            }
        }

        DataSource parse(Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("source")) {
                throw new NcdfGeneratorException("Not source node");
            }

            for (Node child : new NodeWrapper(node)) {
                if (Helper.isElementNode(child)) {
                    extractValue(child);
                }
            }

            return new DataSource(dataStoreName, virtualDataTable, virtualInstanceTable);
        }
    }

    class FilenameTemplateParser {
        private String sql;

        private void extractValue(Node child) throws NcdfGeneratorException {
            String tag = child.getNodeName();
            if (tag.equals("sql")) {
                sql = Helper.nodeVal(child);
            }
        }

        FilenameTemplate parse(Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("filename")) {
                throw new NcdfGeneratorException("Not a filename");
            }

            // extract from nested tags
            for (Node child : new NodeWrapper(node)) {
                extractValue(child);
            }

            // extract from attributes
            for (Node child : new AttrWrapper(node)) {
                extractValue(child);
            }

            return new FilenameTemplate(sql);
        }
    }

    class DimensionsParser {
        List<IDimension> parse(Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("dimensions")) {
                throw new NcdfGeneratorException("Not a dimensions node");
            }

            List<IDimension> dimensions = new ArrayList<IDimension>();
            for (Node child : new NodeWrapper(node)) {
                if (Helper.isElementNode(child)) {
                    String tag = child.getNodeName();
                    if (tag.equals("dimension")) {
                        dimensions.add(new DimensionParser().parse(child));
                    }
                    else {
                        throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
                    }
                }
            }
            return dimensions;
        }
    }

    class DimensionParser {
        IDimension parse(Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("dimension")) {
                throw new NcdfGeneratorException("Not a dimension node");
            }

            String name = "";
            for (Node child : new AttrWrapper(node)) {
                String tag = child.getNodeName();
                if (tag.equals("name")) {
                    name = Helper.nodeVal(child);
                }
                else {
                    throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
                }
            }

            return new DimensionImpl(name, true); // Only supporting a single unlimited dimension at the moment
        }
    }

    class VariableParsers {
        List<IVariable> parse(Context context, Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("variables")) {
                throw new NcdfGeneratorException("Not variables");
            }

            List<IVariable> variables = new ArrayList<IVariable>();
            for (Node child : new NodeWrapper(node)) {
                if (Helper.isElementNode(child)) {
                    String tag = child.getNodeName();
                    if (tag.equals("variable")) {
                        variables.add(new VariableParser().parse(context, child));
                    }
                    else {
                        throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
                    }
                }
            }
            return variables;
        }
    }

    class VariableParser {
        IVariable parse(Context context, Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("variable")) {
                throw new NcdfGeneratorException("Not a variable");
            }

            String name = null;
            IValueEncoder encoder = null;
            List<IDimension> dimensions = new ArrayList<IDimension>(); // support missing dimensions and attributes
            List<Attribute> attributes = new ArrayList<Attribute>();

            for (Node child : new NodeWrapper(node)) {
                if (Helper.isElementNode(child)) {
                    String tag = child.getNodeName();
                    if (tag.equals("name")) {
                        name = Helper.nodeVal(child);
                    }
                    else if (tag.equals("encoder")) {
                        encoder = new EncoderParser().parse(child);
                    }
                    else if (tag.equals("dimensions")) {
                        dimensions = new VariableDimensionsParser().parse(context, child);
                    }
                    else if (tag.equals("attributes")) {
                        attributes = new AttributesParser().parse(child);
                    }
                    else {
                        throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
                    }
                }
            }

            return new Variable(name, dimensions, encoder, attributes);
        }
    }

    class EncoderParser {
        IValueEncoder parse(Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("encoder")) {
                throw new NcdfGeneratorException("Not an encoder");
            }

            // if we need more encoder detail, then can deal with separately
            String tag = Helper.nodeVal(node);
            if (tag.equals("integer")) {
                return new IntValueEncoder();
            }
            else if (tag.equals("float")) {
                return new FloatValueEncoder();
            }
            else if (tag.equals("double")) {
                return new DoubleValueEncoder();
            }
            else if (tag.equals("char")) {
                return new CharValueEncoder();
            }
            else if (tag.equals("byte")) {
                return new ByteValueEncoder();
            }
            else if (tag.equals("time")) {
                return new TimestampValueEncoder();
            }
            else {
                throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
            }
        }
    }

    class VariableDimensionParser {
        IDimension parse(Context context, Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("dimension")) {
                throw new NcdfGeneratorException("Not a dimension");
            }

            for (Node child : new AttrWrapper(node)) {
                String tag = child.getNodeName();
                if (tag.equals("name")) {
                    return context.getDimensionByName(Helper.nodeVal(child));
                }
                else {
                    throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
                }
            }
            return null;
        }
    }

    class VariableDimensionsParser {
        List<IDimension> parse(Context context, Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("dimensions")) {
                throw new NcdfGeneratorException("Not a dimensions node");
            }

            List<IDimension> dimensions = new ArrayList<IDimension>();
            for (Node child : new NodeWrapper(node)) {
                if (Helper.isElementNode(child)) {
                    String tag = child.getNodeName();
                    if (tag.equals("dimension")) {
                        dimensions.add(new VariableDimensionParser().parse(context, child));
                    }
                    else {
                        throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
                    }
                }
            }
            return dimensions;
        }
    }

    class AttributesParser {
        List<Attribute> parse(Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("attributes")
                && !node.getNodeName().equals("globalattributes")
                ) {
                throw new NcdfGeneratorException("Not an attributes node");
            }

            List<Attribute> attributes = new ArrayList<Attribute>();
            for (Node child : new NodeWrapper(node)) {
                if (Helper.isElementNode(child)) {
                    String tag = child.getNodeName();
                    if (tag.equals("attribute")) {
                        attributes.add(new AttributeParser().parse(child));
                    }
                    else {
                        throw new NcdfGeneratorException("Unrecognized tag '" + tag + "'");
                    }
                }
            }
            return attributes;
        }
    }

    class AttributeParser {
        private String name;
        private String value;
        private String sql;

        private void extractValue(Node child) throws NcdfGeneratorException {
            // attr or node
            String tag = child.getNodeName();
            if (tag.equals("name")) {
                name = Helper.nodeVal(child);
            }
            else if (tag.equals("value")) {
                value = Helper.nodeVal(child);
            }
            else if (tag.equals("sql")) {
                sql = Helper.nodeVal(child);
            }
        }

        Attribute parse(Node node) throws NcdfGeneratorException {
            if (!node.getNodeName().equals("attribute")) {
                throw new NcdfGeneratorException("Not an attribute");
            }

            // extract from nested tags
            for (Node child : new NodeWrapper(node)) {
                extractValue(child);
            }

            // extract from attributes
            for (Node child : new AttrWrapper(node)) {
                extractValue(child);
            }

            return new Attribute(name, value, sql);
        }
    }
}

