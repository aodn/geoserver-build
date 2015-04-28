
package au.org.emii.ncdfgenerator.cql;

public interface IExpression {
    int getPosition();
    void accept(IExprVisitor v) throws Exception; // TODO should be CQLExpression only
}

