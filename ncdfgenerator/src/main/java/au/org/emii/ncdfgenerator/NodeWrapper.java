package au.org.emii.ncdfgenerator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class NodeWrapper implements Iterable<Node> {
    // xml helper class

    private Node node;
    private List<Node> nodes;

    public NodeWrapper(Node node) {
        this.node = node;
    }

    public Iterator<Node> iterator() {
        if (nodes == null) {
            NodeList nodeList = node.getChildNodes();
            nodes = new ArrayList<Node>(nodeList.getLength());
            for (int i = 0; i < nodeList.getLength(); i++) {
                nodes.add(nodeList.item(i));
            }
        }

        return nodes.iterator();
    }
}



