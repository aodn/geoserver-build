/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.w3c.dom.Node;

public class FilterParser {

    private NodeWrapper filterNode;

    public FilterParser(Node filterNode) {
        this.filterNode = new NodeWrapper(filterNode);
    }

    public Filter parse() {
        Filter filter = new Filter();
        filter.setEnabled(Boolean.TRUE);

        for (Node node : filterNode) {
            setFilterProperty(filter, node);
        }

        return filter;
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

    private void setFilterProperty(Filter filter, Node node) {
        if (isElement(node)) {
            getFilterPropertySetter(node.getNodeName()).setFilterProperty(filter, getNodeValue(node), node.getNodeName());
        }
    }

    private FilterPropertySetter getFilterPropertySetter(String property) {
        FilterPropertySetter setter = new NullFilterSetter();
        if ("name".equals(property)) {
            setter = new FilterNameSetter();
        }
        else if ("type".equals(property)) {
            setter = new FilterTypeSetter();
        }
        else if ("label".equals(property)) {
            setter = new FilterLabelSetter();
        }
        else if ("visualised".equals(property)) {
            setter = new FilterVisualisedSetter();
        }
        else if ("excludedFromDownload".equals(property)) {
            setter = new FilterExcludedFromDownloadSetter();
        }
        else {
            setter = new FilterExtrasSetter();
        }

        return setter;
    }

    abstract class FilterPropertySetter {

        public abstract void setFilterProperty(Filter filter, String value, String nodeName);
    }

    class FilterNameSetter extends FilterPropertySetter {

        public void setFilterProperty(Filter filter, String value, String nodeName) {
            filter.setName(value);
        }
    }

    class FilterTypeSetter extends FilterPropertySetter {

        public void setFilterProperty(Filter filter, String value, String nodeName) {
            filter.setType(value);
        }
    }

    class FilterLabelSetter extends FilterPropertySetter {

        public void setFilterProperty(Filter filter, String value, String nodeName) {
            filter.setLabel(value);
        }
    }

    class FilterVisualisedSetter extends FilterPropertySetter {

        public void setFilterProperty(Filter filter, String value, String nodeName) {
            filter.setVisualised(Boolean.valueOf(value));
        }
    }

    class FilterExcludedFromDownloadSetter extends FilterPropertySetter {

        public void setFilterProperty(Filter filter, String value, String nodeName) {
            filter.setExcludedFromDownload(Boolean.valueOf(value));
        }
    }

    class FilterExtrasSetter extends FilterPropertySetter {

        public void setFilterProperty(Filter filter, String value, String nodeName) {
            filter.setExtrasField(value, nodeName);
        }
    }

    class NullFilterSetter extends FilterPropertySetter {

        public void setFilterProperty(Filter filter, String value, String nodeName) { }
    }
}
