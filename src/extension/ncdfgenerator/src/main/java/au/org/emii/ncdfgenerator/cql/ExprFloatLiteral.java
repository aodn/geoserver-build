package au.org.emii.ncdfgenerator.cql;

public class ExprFloatLiteral implements IExpression {
    private final int pos;
    private final float value;

    public ExprFloatLiteral(int pos, float value) {
        this.pos = pos;
        this.value = value;
    }

    public final int getPosition() {
        return pos;
    }

    public final float getValue() {
        return value;
    }

    public final void accept(IExprVisitor v) throws Exception {
        v.visit(this);
    }
}
