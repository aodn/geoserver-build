
package au.org.emii.ncdfgenerator.cql;

public class ExprSymbol implements IExpression {
    private final int pos;
    private final String value;

    public ExprSymbol(int pos, String value) {
        this.pos = pos;
        this.value = value;
    }

    public final int getPosition() {
        return pos;
    }
    public final String getValue() {
        return value;
    }

    public final void accept(IExprVisitor v) throws Exception {
        v.visit(this);
    }
}
