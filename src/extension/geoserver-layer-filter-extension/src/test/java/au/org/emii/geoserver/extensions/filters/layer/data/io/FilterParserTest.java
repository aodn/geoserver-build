/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class FilterParserTest {

    @Test
    public void parseTest() {
        Node mockNameNode = getNameNode();
        Node mockTypeNode = getTypeNode();
        Node mockLabelNode = getLabelNode();
        Node mockVisualiseNode = getVisualisedNode();

        NodeList filterNodeList = new MockNodeList(mockNameNode, mockTypeNode, mockLabelNode, mockVisualiseNode);

        Node filterNode = createNiceMock(Node.class);
        expect(filterNode.hasChildNodes()).andReturn(true);
        expect(filterNode.getChildNodes()).andReturn(filterNodeList);

        replay(filterNode);

        FilterParser parser = new FilterParser(filterNode);
        Filter filter = parser.parse();

        assertEquals("deployment_name", filter.getName());
        assertEquals("String", filter.getType());
        assertEquals("Deployment Name", filter.getLabel());
        assertEquals(Boolean.TRUE, filter.getVisualised());
    }

    private Node getNameNode() {
        Node mockChildNode = createNiceMock(Node.class);
        expect(mockChildNode.getNodeValue()).andReturn("deployment_name");

        Node mockNameNode = createNiceMock(Node.class);
        expect(mockNameNode.getNodeName()).andReturn("name");
        expect(mockNameNode.getFirstChild()).andReturn(mockChildNode).times(2);
        expect(mockNameNode.getNodeType()).andReturn(Node.ELEMENT_NODE);

        replay(mockChildNode, mockNameNode);

        return mockNameNode;
    }

    private Node getTypeNode() {
        Node mockChildNode = createNiceMock(Node.class);
        expect(mockChildNode.getNodeValue()).andReturn("String");

        Node mockTypeNode = createNiceMock(Node.class);
        expect(mockTypeNode.getNodeName()).andReturn("type");
        expect(mockTypeNode.getFirstChild()).andReturn(mockChildNode).times(2);
        expect(mockTypeNode.getNodeType()).andReturn(Node.ELEMENT_NODE);

        replay(mockChildNode, mockTypeNode);

        return mockTypeNode;
    }

    private Node getLabelNode() {
        Node mockChildNode = createNiceMock(Node.class);
        expect(mockChildNode.getNodeValue()).andReturn("Deployment Name");

        Node mockLabelNode = createNiceMock(Node.class);
        expect(mockLabelNode.getNodeName()).andReturn("label");
        expect(mockLabelNode.getFirstChild()).andReturn(mockChildNode).times(2);
        expect(mockLabelNode.getNodeType()).andReturn(Node.ELEMENT_NODE);

        replay(mockChildNode, mockLabelNode);

        return mockLabelNode;
    }

    private Node getVisualisedNode() {
        Node mockChildNode = createNiceMock(Node.class);
        expect(mockChildNode.getNodeValue()).andReturn("true");

        Node mockVisualiseNode = createNiceMock(Node.class);
        expect(mockVisualiseNode.getNodeName()).andReturn("visualised");
        expect(mockVisualiseNode.getFirstChild()).andReturn(mockChildNode).times(2);
        expect(mockVisualiseNode.getNodeType()).andReturn(Node.ELEMENT_NODE);

        replay(mockChildNode, mockVisualiseNode);

        return mockVisualiseNode;
    }

    class MockNodeList implements NodeList {

        ArrayList<Node> nodes;

        public MockNodeList(Node name, Node type, Node label, Node visualised) {
            nodes = new ArrayList<Node>();
            nodes.add(name);
            nodes.add(type);
            nodes.add(label);
            nodes.add(visualised);
        }

        public Node item(int index) {
            return nodes.get(index);
        }

        public int getLength() {
            return 4;
        }
    }
}
