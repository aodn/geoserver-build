/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class NodeWrapperTest {

    private Node mockNode;
    private Node mockChildNode;
    private NodeList mockNodeList;

    @Before
    public void setUpNode() {
        mockNode = createNiceMock(Node.class);
        mockChildNode = createNiceMock(Node.class);
        mockNodeList = createNiceMock(NodeList.class);
    }

    @Test
    public void hasNoChildNodesTest() {
        expect(mockNode.hasChildNodes()).andReturn(false);
        NodeWrapper node = new NodeWrapper(mockNode);

        int i = 0;
        for (Node n : node) {
            i++;
        }

        assertEquals(0, i);
    }

    @Test
    public void hasChildNodesTest() {
        expect(mockNodeList.getLength()).andReturn(1).times(2);
        expect(mockNodeList.item(0)).andReturn(mockChildNode);
        expect(mockNode.hasChildNodes()).andReturn(true);
        expect(mockNode.getChildNodes()).andReturn(mockNodeList);

        NodeWrapper node = new NodeWrapper(mockNode);

        replay(mockChildNode, mockNodeList, mockNode);

        int i = 0;
        Node result = null;
        for (Node n : node) {
            i++;
            result = n;
        }

        verify(mockNode);
        verify(mockNodeList);

        assertEquals(1, i);
        assertEquals(mockChildNode, result);
    }
}
