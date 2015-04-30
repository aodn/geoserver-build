

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
        b.append("'"+ expr.getValue() + "'");
    }

    public final void visit(ExprSymbol expr) {
        // must quote field to enforce full case handling
        b.append('\"' + expr.getValue() + '\"');
    }

    public final void visit(ExprWKTLiteral expr) throws Exception {
        b.append("ST_GeomFromText( '" + expr.getValue() + "', 4326)");
    }


    public final void visit(ExprProc expr) throws Exception {
        String symbol = expr.getSymbol();
        String lower = symbol.toLowerCase();

        if(symbol.equals("nop")) {
            if(expr.getChildren().size() != 1) {
                // should almost be an unchecked runtime exception
                throw new CQLException("nop with more than one child");
            }
            expr.getChildren().get(0).accept(this);
        } else if(lower.equals("and")
                || lower.equals("or")) {
            emitInfixSqlExpr(symbol, expr);
        } else if(lower.equals("intersects")) {
            emitFunctionSqlExpr("ST_INTERSECTS", expr);
        } else if(symbol.equals(">=")
                || symbol.equals("<=")
                || symbol.equals("<")
                || symbol.equals(">")
                || symbol.equals("=")
                || symbol.equals("<>")) {
            emitInfixSqlExpr(symbol, expr);
        } else {
            throw new CQLException("Unrecognized proc expression symbol '" + symbol + "'");
        }
    }

    public final void emitFunctionSqlExpr(String op, ExprProc expr) throws Exception {
        // if expansion is done in order we may be ok,....

        b.append(op);
        b.append('(');

        boolean first = true;
        for(IExpression child : expr.getChildren()) {
            if(first) {
                child.accept(this);
                first = false;
            } else {
                b.append(',');
                child.accept(this);
            }
        }
        b.append(')');
    }


    public final void emitInfixSqlExpr(String op, ExprProc expr) throws Exception {
        // if expansion is done in order we may be ok,....
        b.append('(');
        expr.getChildren().get(0).accept(this);
        b.append(' ');
        b.append(op);
        b.append(' ');
        expr.getChildren().get(1).accept(this);
        b.append(')');
    }
}

