package au.org.emii.wps.catalogue;

public class CatalogueReaderConfig {

    private String catalogueUrl;
    private String layerSearchField;

    public CatalogueReaderConfig(String catalogueUrl, String layerSearchField) {
        this.catalogueUrl = catalogueUrl;
        this.layerSearchField = layerSearchField;
    }

    public String getCatalogueUrl() {
        return catalogueUrl;
    }

    public String getLayerSearchField() {
        return layerSearchField;
    }
}
