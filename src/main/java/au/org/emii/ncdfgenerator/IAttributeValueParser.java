package au.org.emii.ncdfgenerator;

interface IAttributeValueParser {
    AttributeValue parse(String s) throws NcdfGeneratorException;
}

