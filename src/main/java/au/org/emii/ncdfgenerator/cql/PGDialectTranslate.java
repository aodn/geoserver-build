
package au.org.emii.ncdfgenerator.cql;

public class PGDialectTranslate implements IDialectTranslate {
    // we have to have something to instantiate the specific visitor
    public PGDialectTranslate() {
        // Ideally this would be Non PG specific and we should pass the PGDialectSelectionGenerator on the constructor
        // however the issue is that it needs the stringBuilder to assemble
        // which would require creating a property setter.
    }

    public final String process(IExpression expr) throws Exception {
        StringBuilder b = new StringBuilder();
        expr.accept(new PGDialectSelectionGenerator(b));

        return b.toString();
    }
}
