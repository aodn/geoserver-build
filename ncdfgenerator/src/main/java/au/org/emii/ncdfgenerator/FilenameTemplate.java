package au.org.emii.ncdfgenerator;

class FilenameTemplate {
    private final String sql;

    public FilenameTemplate(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}

