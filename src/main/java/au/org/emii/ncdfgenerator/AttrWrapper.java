
package au.org.emii.ncdfgenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;


class AttrWrapper implements Iterable<Node> {
    // xml helper class

    private Node node;
    private List<Node> nodes;

    public AttrWrapper(Node node) {
        this.node = node;
    }

    public Iterator<Node> iterator() {
        if(nodes == null) {
            NamedNodeMap attrs = node.getAttributes();
            nodes = new ArrayList<Node>(attrs.getLength());
            for(int i = 0; i < attrs.getLength(); ++i)
                nodes.add(attrs.item(i));
        }

        return nodes.iterator();
    }
}

