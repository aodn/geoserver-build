package au.org.emii.ncdfgenerator.cql;

import java.util.ArrayList;
import java.util.List;

public class ExprProc implements IExpression {
    private final int pos;
    // public for unit tests
    private final String symbol;
    private final ArrayList<IExpression> children;

    public ExprProc(int pos, String symbol, ArrayList<IExpression> children) {
        this.pos = pos;
        this.symbol = symbol;
        this.children = children;
    }

    public final int getPosition() {
        return pos;
    }

    public final String getSymbol() {
        return symbol;
    }

    public final List<IExpression> getChildren() {
        return children;
    }

    public final void accept(IExprVisitor v) throws Exception {
        v.visit(this);
    }
}
