package au.org.emii.ncdfgenerator.cql;

public interface IDialectTranslate {
    String process(IExpression expr) throws Exception;
}

