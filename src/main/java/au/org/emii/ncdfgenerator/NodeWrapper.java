
package au.org.emii.ncdfgenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


class NodeWrapper implements Iterable<Node> {
    // xml helper class

    private Node node;
    private List<Node> nodes;
    private NodeList nodeList;

    public NodeWrapper(Node node) {
        this.node = node;
    }

    public Iterator<Node> iterator() {
        if(nodes == null) {
            buildNodes();
        }

        return nodes.iterator();
    }

    private void buildNodes() {
        nodes = new ArrayList<Node>(getListLength());
        for(int i = 0; i < getListLength(); i++) {
            nodes.add(nodeList.item(i));
        }
    }

    private int getListLength() {
        return getNodeList().getLength();
    }

    private NodeList getNodeList() {
        if(nodeList == null) {
            setNodeList();
        }
        return nodeList;
    }

    private void setNodeList() {
        if(node.hasChildNodes()) {
            nodeList = node.getChildNodes();
        } else {
            nodeList = new NullNodeList();
        }
    }

    private class NullNodeList implements NodeList {

        public Node item(int index) {
            return null;
        }

        public int getLength() {
            return 0;
        }
    }
}



