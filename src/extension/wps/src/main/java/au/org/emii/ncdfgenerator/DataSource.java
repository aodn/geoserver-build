package au.org.emii.ncdfgenerator;

public class DataSource {
    private final String dataStoreName;
    private final String virtualDataTable;
    private final String virtualInstanceTable;

    public DataSource(String dataStoreName, String virtualDataTable, String virtualInstanceTable) {
        this.dataStoreName = dataStoreName;
        this.virtualDataTable = virtualDataTable;
        this.virtualInstanceTable = virtualInstanceTable;
    }

    public String getDataStoreName() {
        return dataStoreName;
    }

    public String getVirtualDataTable() {
        return virtualDataTable;
    }

    public String getVirtualInstanceTable() {
        return virtualInstanceTable;
    }
}

