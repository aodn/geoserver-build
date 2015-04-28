
package au.org.emii.ncdfgenerator.cql;

public interface IExprParser {
    IExpression parseExpression(String s) throws CQLException;
}

