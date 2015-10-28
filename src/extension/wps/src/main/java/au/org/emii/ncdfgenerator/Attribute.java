package au.org.emii.ncdfgenerator;

class Attribute {
    private final String name;
    private final String value;
    private final String sql;

    public Attribute(String name, String value, String sql) {
        this.name = name;
        this.value = value;
        this.sql = sql;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getSql() {
        return sql;
    }
}

