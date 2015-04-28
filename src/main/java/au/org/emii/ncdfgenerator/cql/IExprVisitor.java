
package au.org.emii.ncdfgenerator.cql;

public interface IExprVisitor {
    void visit(ExprIntegerLiteral expr) throws Exception;
    void visit(ExprFloatLiteral expr) throws Exception;
    void visit(ExprStringLiteral expr) throws Exception;
    void visit(ExprTimestampLiteral expr) throws Exception;
    void visit(ExprWKTLiteral expr) throws Exception;
    void visit(ExprSymbol expr) throws Exception;
    void visit(ExprProc expr) throws Exception;
}


