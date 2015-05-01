
package au.org.emii.ncdfgenerator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import au.org.emii.ncdfgenerator.cql.*;

public class QueryTest {

    @Before
    public void before() {
    }

    private IExpression doExprTest(String s) throws Exception {
        IExprParser p =	new ExprParser() ;
        IExpression expr = p.parseExpression(s);
        assertTrue(expr != null);
        return expr;
    }

    @Test
    public void testIntegerLiteral() throws Exception {
        IExpression expr = doExprTest("1234") ;
        assertTrue(expr instanceof ExprIntegerLiteral);
    }

    @Test
    public void testFloatLiteral() throws Exception {
        IExpression expr = doExprTest("123.5");
        assertTrue(expr instanceof ExprFloatLiteral);
    }

    @Test(expected = CQLException.class)
    public void testFloatLiteralNoDecimalComponent() throws Exception {
        doExprTest("1234.");
    }

    @Test(expected = CQLException.class)
    public void testFloatLiteralNoDecimalComponentWithASpace() throws Exception {
        doExprTest("1234. ");
    }

    @Test(expected = CQLException.class)
    public void testFloatLiteralNoIntegerComponent() throws Exception {
        doExprTest(".234");
    }

    @Test
    public void testFloatLiteralWithSpaces() throws Exception {
        IExpression expr = doExprTest("1234.0 ");
        assertTrue(expr instanceof ExprFloatLiteral);
    }

    @Test
    public void testStringLiteral() throws Exception
    {
        IExpression expr = doExprTest("'string'");
        assertTrue(expr instanceof ExprStringLiteral);
    }

    @Test(expected = CQLException.class)
    public void testStringLiteralWithNoStartingInvertedComma() throws Exception
    {
        doExprTest("string'");
    }

    @Test(expected = CQLException.class)
    public void testStringLiteralWithNoEndingInvertedComma() throws Exception
    {
        doExprTest("'string");
    }

    @Test
    public void testTimestampLiteral() throws Exception {
        IExpression expr = doExprTest(" '2015-01-13T23:00:00Z' ") ;
        // will be an ExprProc nop, due to the way we parse this thing, parsing the quotes separately
        ExprProc expr0 = (ExprProc)expr;
        IExpression expr1 = expr0.getChildren().get(0);
        assertTrue(expr1 instanceof ExprTimestampLiteral);
    }

    @Test
    public void testWKT() throws Exception {
        IExpression expr = doExprTest("POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))");
        assertTrue(expr instanceof ExprWKTLiteral);
    }

    @Test
    public void testSymbol() throws Exception {
        IExpression expr = doExprTest("TIME") ;
        assertTrue(expr instanceof ExprSymbol);
    }

    @Test
    public void testPrecedenceBinding01() throws Exception {
        IExpression expr = doExprTest(" TIME>='2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' AND 777= 999") ;
        assertTrue(expr instanceof ExprProc);
        ExprProc expr0 = (ExprProc) expr;
        assertTrue(expr0.getSymbol().equals("AND"));
        // TODO check lhs for precendence
    }

    @Test
    public void testPrecedenceBinding02() throws Exception {
        IExpression expr = doExprTest(" 123 <= SYM AND 666 > 777 OR 888 = '2015-01-13T23:00:00Z' ");
        assertTrue(expr instanceof ExprProc);
        // TODO check lhs for precendence
    }


    @Test
    public void testFunction() throws Exception {
        IExpression expr = doExprTest(" myname( 123 , sym )  ");
        assertTrue(expr instanceof ExprProc);
    }

    @Test
    public void testSubsetServiceExampleQuery() throws Exception {
        // example cql filter query expression from
        // https://github.com/aodn/netcdf-subset-service
        String s = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";
        doExprTest(s);
    }

    @Test(expected = CQLException.class)
    public void testParseThatShouldThrow() throws Exception {
        String s = " and ( 2013-6 ) ";
        IExprParser p =	new ExprParser() ;
        p.parseExpression(s);
    }

    @Test
    public void testRewriter() throws Exception {
        // translate all the way to sql. test checks that succeeds nothing throws

        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";
        IExpression expr = doExprTest(cql);
        IDialectTranslate dt = new PGDialectTranslate();
        dt.process(expr);
        // TODO ...
    }

    @Test
    public void testSimpleNegation() throws Exception {
        IExpression expr = doExprTest("-123");
        assertTrue(expr instanceof ExprProc);
        new PGDialectTranslate().process(expr);
    }

    @Test
    public void testComplexUnary() throws Exception {
        // test all the way to sql translation
        IExpression expr = doExprTest("SYM <  +(-+ -123)");
        assertTrue(expr instanceof ExprProc);
        String sql = new PGDialectTranslate().process(expr);
        assertEquals(sql, "(\"SYM\" < (+(-(+(-123)))))");
    }
}

