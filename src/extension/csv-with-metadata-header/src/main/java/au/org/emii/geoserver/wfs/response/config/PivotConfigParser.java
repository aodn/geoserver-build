/*
 * Copyright 2021 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.wfs.response.config;

import au.org.emii.geoserver.extensions.filters.layer.data.io.NodeWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class PivotConfigParser {

    private final NodeWrapper pivotConfigNode;

    public PivotConfigParser(Document document) {
        this.pivotConfigNode = new NodeWrapper(document.getFirstChild());
    }

    public PivotConfig parse() {
        PivotConfig config = new PivotConfig();

        for (Node node : pivotConfigNode) {
            setConfigProperty(config, node);
        }

        return config;
    }

    private boolean isElement(Node node) {
        return Node.ELEMENT_NODE == node.getNodeType();
    }

    private String getNodeValue(Node node) {
        if (node.getFirstChild() != null) {
            return node.getFirstChild().getNodeValue();
        }

        return "";
    }

    private void setConfigProperty(PivotConfig config, Node node) {
        if (isElement(node)) {
            getConfigPropertySetter(node.getNodeName()).setConfigProperty(config, getNodeValue(node), node.getNodeName());
        }
    }

    private ConfigPropertySetter getConfigPropertySetter(String property) {
        ConfigPropertySetter setter = new NullConfigSetter();
        if ("orderDirection".equals(property)) {
            setter = new OrderDirectionSetter();
        }
        else if ("defaultValue".equals(property)) {
            setter = new DefaultValueSetter();
        }
        else if ("excludedField".equals(property)) {
            setter = new ExcludedFieldSetter();
        }

        return setter;
    }

    abstract class ConfigPropertySetter {

        public abstract void setConfigProperty(PivotConfig pivotConfig, String value, String nodeName);
    }

    class OrderDirectionSetter extends ConfigPropertySetter {

        public void setConfigProperty(PivotConfig pivotConfig, String value, String nodeName) {
            pivotConfig.setOrderDirection(value);
        }
    }

    class DefaultValueSetter extends ConfigPropertySetter {

        public void setConfigProperty(PivotConfig pivotConfig, String value, String nodeName) {
            pivotConfig.setDefaultValue(value);
        }
    }

    class ExcludedFieldSetter extends ConfigPropertySetter {

        public void setConfigProperty(PivotConfig pivotConfig, String value, String nodeName) {
            pivotConfig.addExcludedField(value);
        }
    }

    class NullConfigSetter extends ConfigPropertySetter {

        public void setConfigProperty(PivotConfig pivotConfig, String value, String nodeName) { }
    }
}
