package au.org.emii.ncdfgenerator.cql;

import java.text.SimpleDateFormat;

public class PGDialectSelectionGenerator implements IExprVisitor {
    private final StringBuilder b;
    private final SimpleDateFormat df;

    public PGDialectSelectionGenerator(StringBuilder b) {
        this.b = b;
        this.df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    // think our naming is incorrect
    public final void visit(ExprIntegerLiteral expr) {
        // This should actually emit a '?' and load the value into the sql parameter list
        // to avoid sql injection
        b.append(expr.getValue());
    }

    public final void visit(ExprFloatLiteral expr) {
        b.append(expr.getValue());
    }

    public final void visit(ExprTimestampLiteral expr) {
        b.append("'" + df.format(expr.getValue()) + "'");
    }

    public final void visit(ExprStringLiteral expr) {
        b.append("'" + expr.getValue() + "'");
    }

    public final void visit(ExprSymbol expr) {
        // must quote field to enforce full case handling
        b.append('\"' + expr.getValue() + '\"');
    }

    public final void visit(ExprWKTLiteral expr) throws Exception {
        b.append("ST_GeomFromText( '" + expr.getValue() + "', 4326)");
    }

    public final void visit(ExprProc expr) throws Exception {
        String symbol = expr.getSymbol().toLowerCase();
        int arity = expr.getChildren().size();

        if (arity == 1 && symbol.equals("nop")) {
            expr.getChildren().get(0).accept(this);
        }
        else if (symbol.equals("intersects")) {
            emitFunctionSqlExpr("ST_INTERSECTS", expr);
        }
        else if (arity == 2
            && (
            symbol.equals("and")
                || symbol.equals("or")
                || symbol.equals(">=")
                || symbol.equals("<=")
                || symbol.equals("<")
                || symbol.equals(">")
                || symbol.equals("=")
                || symbol.equals("<>"))) {
            emitInfixSqlExpr(symbol, expr);
        }
        else if (arity == 1
            && (symbol.equals("+")
            || symbol.equals("-"))) {
            emitUnarySqlExpr(symbol, expr);
        }
        else {
            throw new CQLException("Unrecognized proc expression symbol '" + symbol + "'");
        }
    }

    private void emitFunctionSqlExpr(String op, ExprProc expr) throws Exception {
        b.append(op);
        b.append('(');
        for (IExpression child : expr.getChildren()) {
            if (child != expr.getChildren().get(0)) {
                b.append(',');
            }
            child.accept(this);
        }
        b.append(')');
    }

    private void emitUnarySqlExpr(String op, ExprProc expr) throws Exception {
        b.append('(');
        b.append(op);
        expr.getChildren().get(0).accept(this);
        b.append(')');
    }

    private void emitInfixSqlExpr(String op, ExprProc expr) throws Exception {
        b.append('(');
        expr.getChildren().get(0).accept(this);
        b.append(' ');
        b.append(op);
        b.append(' ');
        expr.getChildren().get(1).accept(this);
        b.append(')');
    }
}

