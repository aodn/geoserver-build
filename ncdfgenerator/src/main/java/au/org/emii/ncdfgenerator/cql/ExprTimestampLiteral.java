package au.org.emii.ncdfgenerator.cql;

import java.sql.Timestamp;

public class ExprTimestampLiteral implements IExpression {
    private final int pos;
    private final Timestamp value;

    public ExprTimestampLiteral(int pos, Timestamp value) {
        this.pos = pos;
        this.value = value;
    }

    public final int getPosition() {
        return pos;
    }

    public final Timestamp getValue() {
        return value;
    }

    public final void accept(IExprVisitor v) throws Exception {
        v.visit(this);
    }
}
