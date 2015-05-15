package au.org.emii.ncdfgenerator.cql;

public class ExprIntegerLiteral implements IExpression {
    private final int pos;
    private final int value;

    public ExprIntegerLiteral(int pos, int value) {
        this.pos = pos;
        this.value = value;
    }

    public final int getPosition() {
        return pos;
    }

    public final int getValue() {
        return value;
    }

    public final void accept(IExprVisitor v) throws Exception {
        v.visit(this);
    }
}
