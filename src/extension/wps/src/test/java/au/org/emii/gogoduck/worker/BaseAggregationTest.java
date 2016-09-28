package au.org.emii.gogoduck.worker;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class BaseAggregationTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseAggregationTest.class);

    String format = "application/x-netcdf";
    String outputFilePath = "src/test/resources/aggregation-files/result.nc";

    public abstract void aggregationTest() throws Exception;

    protected void verifyVariables(List<Variable> variables1, List<Variable> variables2) {
        Iterator<Variable> ncVarItr1 = variables1.iterator();
        Iterator<Variable> ncVarItr2 = variables2.iterator();

        while (ncVarItr1.hasNext() && ncVarItr2.hasNext()) {
            Variable ncVar1 = ncVarItr1.next();
            Variable ncVar2 = ncVarItr2.next();
            logger.info(String.format("Variable 1:%s", ncVar1.getNameAndDimensions()));
            logger.info(String.format("Variable 2:%s", ncVar2.getNameAndDimensions()));
            Assert.assertEquals(ncVar1.getNameAndDimensions(), ncVar2.getNameAndDimensions());

            List<Dimension> dimensions1 = ncVar1.getDimensions();
            List<Dimension> dimensions2 = ncVar2.getDimensions();
            verifyDimensions(dimensions1, dimensions2);

            List<Attribute> globalAttributes1 = ncVar1.getAttributes();
            List<Attribute> globalAttributes2 = ncVar2.getAttributes();
            verifyAttributes(globalAttributes1, globalAttributes2);
        }
    }

    protected void verifyDimensions(List<Dimension> dimensions1, List<Dimension> dimensions2) {
        Iterator<Dimension> ncDimItr1 = dimensions1.iterator();
        Iterator<Dimension> ncDimItr2 = dimensions2.iterator();

        while (ncDimItr1.hasNext() && ncDimItr2.hasNext()) {
            Dimension ncDim1 = ncDimItr1.next();
            Dimension ncDim2 = ncDimItr2.next();
            logger.info(String.format("Dimension 1:%s", ncDim1.getShortName()));
            logger.info(String.format("Dimension 2:%s", ncDim2.getShortName()));
            Assert.assertEquals(ncDim1.getShortName(), ncDim2.getShortName());
        }
    }

    protected void verifyAttributes(List<Attribute> globalAttributes1, List<Attribute> globalAttributes2) {

        Map<String, Attribute> attributes1 = getAttributesMap(globalAttributes1);
        Map<String, Attribute> attributes2 = getAttributesMap(globalAttributes2);

        for (String attributeFullName : attributes1.keySet()) {
            if (!attributeFullName.equals("history") && !attributeFullName.equals("NCO")) {
                Attribute ncAtt1 = attributes1.get(attributeFullName);
                Attribute ncAtt2 = attributes2.get(attributeFullName);

                logger.info(String.format("Attribute 1:%s String Value:%s Numeric Value:%s", ncAtt1.getFullName(), ncAtt1.getStringValue(), ncAtt1.getNumericValue()));
                logger.info(String.format("Attribute 2:%s String Value:%s Numeric Value:%s", ncAtt2.getFullName(), ncAtt2.getStringValue(), ncAtt2.getNumericValue()));
                Assert.assertEquals(ncAtt1.getFullName(), ncAtt2.getFullName());
                Assert.assertEquals(ncAtt1.getStringValue(), ncAtt2.getStringValue());
                Assert.assertEquals(ncAtt1.getNumericValue(), ncAtt2.getNumericValue());
            }
        }
    }

    public Map<String, Attribute> getAttributesMap(List<Attribute> globalAttributes) {
        Map<String, Attribute> attributes = new HashMap<>();
        for (Attribute attribute : globalAttributes) {
            attributes.put(attribute.getFullName(), attribute);
        }
        return attributes;
    }

}
