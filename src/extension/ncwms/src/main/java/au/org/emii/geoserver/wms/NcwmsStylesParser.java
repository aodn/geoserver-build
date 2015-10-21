package au.org.emii.geoserver.wms;

import org.dom4j.Document;
import org.dom4j.tree.DefaultText;
import org.dom4j.xpath.DefaultXPath;

import java.util.*;

class NcwmsStylesParser {

    NcwmsStyle parse(Document document, String layerName) {

        DefaultXPath xpath = new DefaultXPath("//x:Layer/x:Title[.=\'" + layerName + "\']/../x:Style/x:Name/text()");
        Map<String, String> namespaces = new TreeMap<String, String>();
        namespaces.put("x", "http://www.opengis.net/wms");
        xpath.setNamespaceURIs(namespaces);

        List<DefaultText> list = xpath.selectNodes(document);

        return new StyleParser().parse(list);
    }

    class StyleParser {

        NcwmsStyle parse(List<DefaultText> list) {

            Set<String> styles = new HashSet<String>();
            Set<String> palettes = new HashSet<String>();

            for (DefaultText text : list) {
                styles.add(text.getText().split("/")[0]);
                palettes.add(text.getText().split("/")[1]);
            }

            ArrayList<String> sortedStyles = new ArrayList<String>(styles);
            Collections.sort(sortedStyles);
            ArrayList<String> sortedPalettes = new ArrayList<String>(palettes);
            Collections.sort(sortedPalettes);

            return new NcwmsStyle(sortedStyles, sortedPalettes);
        }
    }
}
