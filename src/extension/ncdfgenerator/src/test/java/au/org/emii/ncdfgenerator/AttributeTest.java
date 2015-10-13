package au.org.emii.ncdfgenerator;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AttributeTest {

    AttributeValueParser p;

    @Before
    public void before() {
        p = new AttributeValueParser();
    }

    @Test
    public void testByte() throws Exception {
        AttributeValue x = p.parseInteger("123b", 0);
        assertTrue(x.getValue() instanceof Byte);
        assertEquals(x.getPosition(), 4);
    }

    @Test
    public void testInteger() throws Exception {
        AttributeValue x = p.parseInteger("123", 0);
        assertTrue(x.getValue() instanceof Integer);
        assertEquals(x.getPosition(), 3);
    }

    @Test
    public void testDouble() throws Exception {
        AttributeValue x = p.parseFloat("123.", 0);
        assertTrue(x.getValue() instanceof Double);
        assertEquals(x.getPosition(), 4);
    }

    @Test
    public void testFloat() throws Exception {
        AttributeValue x = p.parseFloat("123.f", 0);
        assertTrue(x.getValue() instanceof Float);
        assertEquals(x.getPosition(), 5);
    }

    @Test
    public void testString() throws Exception {
        AttributeValue x = p.parseString("\"value\"", 0);
        assertTrue(x.getValue() instanceof String);
        assertEquals(x.getPosition(), 7);
    }

    @Test
    public void testString2() throws Exception {
        AttributeValue x = p.parseString("'value'", 0);
        assertTrue(x.getValue() instanceof String);
        assertEquals(x.getPosition(), 7);
    }

    @Test
    public void testArray() throws Exception {
        IAttributeValueParser p = this.p;
        AttributeValue x = p.parse("0b, 5b, 7b, 9b");
        Array value = (Array)x.getValue();
        assertTrue(value != null);
        // TODO check length ... etc
        // pos etc.
    }

    @Test
    public void testAttributeValue() throws Exception {
        IAttributeValueParser p = this.p;

        AttributeValue x = p.parse("\"value\"");
        assertTrue(x.getValue() instanceof String);

        x = p.parse("123b");
        assertTrue(x.getValue() instanceof Byte);

        x = p.parse("123.456");
        assertTrue(x.getValue() instanceof Double);
    }
}


