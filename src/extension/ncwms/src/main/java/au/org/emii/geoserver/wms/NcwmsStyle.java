package au.org.emii.geoserver.wms;

import java.util.List;

public class NcwmsStyle {
    private final List<String> styles;
    private final List<String> palettes;

    NcwmsStyle(
        List<String> styles,
        List<String> palettes
    ) {
        this.styles = styles;
        this.palettes = palettes;
    }

    List<String> getStyles() {
        return styles;
    }

    List<String> getPalettes() {
        return palettes;
    }
}

