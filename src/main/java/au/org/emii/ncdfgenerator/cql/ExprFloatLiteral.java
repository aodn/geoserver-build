package au.org.emii.ncdfgenerator.cql;

public class ExprFloatLiteral implements IExpression
{
    final int pos;
    final float value;

    public ExprFloatLiteral(int pos, float value)
    {
        this.pos = pos;
        this.value = value;
    }

    public int getPosition() { return pos; }

    public void accept(IExprVisitor v) throws Exception
    { v.visit(this); }
}
