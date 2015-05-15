package au.org.emii.ncdfgenerator.cql;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class PrettyPrinterVisitor implements IExprVisitor {

    private final OutputStream os;

    public PrettyPrinterVisitor(OutputStream os) {
        this.os = os;
    }

    private void write(String s) throws IOException {
        os.write(s.getBytes(Charset.forName("UTF-8")));
    }

    public final void visit(ExprSymbol expr) throws Exception {
        write("Symbol:" + expr.getValue());
    }

    public final void visit(ExprIntegerLiteral expr) throws Exception {
        write("Integer:" + expr.getValue());
    }

    public final void visit(ExprFloatLiteral expr) throws Exception {
        write("Float:" + expr.getValue());
    }

    public final void visit(ExprTimestampLiteral expr) throws Exception {
        write("Timestamp:" + expr.getValue());
    }

    public final void visit(ExprStringLiteral expr) throws Exception {
        write("Literal:" + expr.getValue());
    }

    public final void visit(ExprWKTLiteral expr) throws Exception {
        write("WKTLiteral:" + expr.getValue());
    }

    public final void visit(ExprProc expr) throws Exception {
        write("(" + expr.getSymbol() + " ");
        for (IExpression child : expr.getChildren()) {
            child.accept(this);
            write(" ");
        }
        write(")");
    }
}

