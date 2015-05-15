package au.org.emii.ncdfgenerator.cql;

public class ExprWKTLiteral implements IExpression {
    private final int pos;
    private final String value;

    public ExprWKTLiteral(int pos, String value) {
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



