package se.sics.sim.examples;

import java.util.Enumeration;
import java.util.Hashtable;
import se.sics.sim.core.Link;
import se.sics.sim.core.Node;
import se.sics.sim.core.Simulator;
import se.sics.sim.interfaces.ApplicationLayer;
import se.sics.sim.interfaces.MacLayer;
import se.sics.sim.interfaces.StateListener;
import se.sics.sim.net.ExtendedLink;
import se.sics.sim.net.ExtendedNode;
import se.sics.sim.net.MessageNode;

/**
 *
 * Created: Fri Apr 27 10:38:39 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class PlacementUtil {

    private final static boolean DEBUG = false;

    public static final int MESH_ALL = 1;
    public static final int MESH_FWD = 2;
    public static final int MESH_FWD_MID = 3;
    public static final int MESH_FWD_MID_BOTTLE = 4;

    public static Node[] setupLine(Simulator sim, int count, Class<?> nodeC) {
        return setupLine(sim, count, nodeC, null, null, null);
    }

    public static Node[] setupLine(Simulator sim, int count, Class<?> nodeC, Class<?> macLayerC, Class<?> applicationC,
            Object policyObject) {

        Node[] nodes = new Node[count];
        for (int i = 0, n = nodes.length; i < n; i++) {
            nodes[i] = createNode(sim, nodeC, macLayerC, applicationC, policyObject);
            if (sim.visualizer != null) {
                // sim.visualizer.addNode(nodes[i].getNodeID());
                sim.visualizer.setLocation(nodes[i].getNodeID(), 50 + i * 40, 50);
            }

            if (i > 1) {
                nodes[i - 1].createLink(nodes[i - 2]);
                nodes[i - 1].createLink(nodes[i]);
            }
        }
        nodes[0].createLink(nodes[1]);
        nodes[nodes.length - 1].createLink(nodes[nodes.length - 2]);

        addLinks(sim, nodes);

        return nodes;
    }

    public static void addLinks(Simulator sim, Node[] nodes) {
        if (sim.visualizer != null) {
            for (int i = 0, n = nodes.length; i < n; i++) {
                int nid = nodes[i].getNodeID();
                Node[] neighbors = nodes[i].getNeighbors();
                for (int j = 0, m = neighbors.length; j < m; j++) {
                    sim.visualizer.addLink(nid, neighbors[j].getNodeID());
                }
            }
        }
    }

    public static Node createNode(Simulator sim, Class<?> nodeC, Class<?> macLayerC, Class<?> applicationC,
            Object policyObject) {

        Node node = null;
        try {
            node = (Node) nodeC.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (macLayerC != null) {
            MacLayer macLayer = null;
            try {
                macLayer = (MacLayer) macLayerC.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            node.setMacLayer(macLayer);
            if (applicationC != null) {
                ApplicationLayer application = null;
                try {
                    application = (ApplicationLayer) applicationC.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                macLayer.addPacketReceiver(application);
                node.setApplicationLayer(application);
                if (policyObject != null) {
                    StateListener stateListener = null;
                    if (policyObject instanceof StateListener) {
                        stateListener = (StateListener) policyObject;
                    } else {
                        try {
                            stateListener = (StateListener) ((Class<?>) policyObject).newInstance();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    node.setStateListener(stateListener);
                }
            }
        }
        sim.addNode(node);
        return node;
    }

    public static Node[] setupBottle(Simulator sim, Class<?> nodeC, int type) {
        Node[] nodes = PlacementUtil.setupMeshD(sim, 2, nodeC, type, 0);
        Node[] nodes2 = PlacementUtil.setupMeshD(sim, 2, nodeC, type, 200);
        nodes[nodes.length - 1].createLink(nodes2[0]);
        nodes2[0].createLink(nodes[nodes.length - 1]);
        // Must also notify visualizer about the new link
        if (sim.visualizer != null) {
            sim.visualizer.addLink(nodes[nodes.length - 1].getNodeID(), nodes2[0].getNodeID());
            sim.visualizer.addLink(nodes2[0].getNodeID(), nodes[nodes.length - 1].getNodeID());
        }

        MessageNode[] nodes3 = new MessageNode[nodes.length + nodes2.length];
        System.arraycopy(nodes, 0, nodes3, 0, nodes.length);
        System.arraycopy(nodes2, 0, nodes3, nodes.length, nodes2.length);
        return nodes3;
    }

    // -------------------------------------------------------------------
    // Sets up a mesh with three multiple paths to sink
    // x x x
    // src x x dst
    // x x x
    // -1- -2- -3- dst (stages)
    // 8*n - 1, 8*n => node %9 7,8, 16,17
    // 1 4 (7) 10 13
    // 0 3 6 9 12 15
    // 2 5 (8) 11 14
    //
    // -------------------------------------------------------------------
    public static Node[] setupMeshD(Simulator sim, int count, Class<?> nodeC, int type, int xoffset) {
        int nodeCount = count * 3 + 1;
        Node[] nodes = new Node[nodeCount];
        for (int i = 0, n = nodeCount; i < n; i++) {
            nodes[i] = createNode(sim, nodeC, null, null, null);
            if (sim.visualizer != null) {
                // sim.visualizer.addNode(nodes[i].getNodeID());
                int x = 50 + (i / 3) * 50 + ((i % 3) > 0 ? 25 : 0) + xoffset;
                int y = 100 + ((i % 3 == 2) ? 50 : (i % 3 == 1) ? -50 : 0);
                sim.visualizer.setLocation(nodes[i].getNodeID(), x, y);
            }
        }
        for (int i = 0, n = nodeCount; i < n; i++) {
            Node node = nodes[i];

            if (i % 3 == 0) {
                // A middle node!
                if (i > 0) {
                    node.createLink(nodes[i - 3]); // middle node one stage back
                    if (type != MESH_FWD || i == nodeCount - 1) {
                        node.createLink(nodes[i - 2]); // upper node one stage
                                                       // back
                        node.createLink(nodes[i - 1]); // lower node one stage
                                                       // back
                    }
                }
                // Last stage does only consist of 1 node...
                if (i < nodeCount - 1) {
                    if (type != MESH_FWD || i < 3) {
                        node.createLink(nodes[i + 1]); // upper node this stage
                        node.createLink(nodes[i + 2]); // lower node this stage
                    }
                }
                if (i < nodeCount - 3) {
                    // Forward it is only possible to hear the middle node!
                    node.createLink(nodes[i + 3]); // middle node one stage
                                                   // forward!
                }
            } else if (i % 3 == 1) {
                // An upper node!
                if (type != MESH_FWD) {
                    node.createLink(nodes[i - 1]); // mid node this stage

                    if (type == MESH_ALL)
                        node.createLink(nodes[i + 1]); // lower node this stage

                    node.createLink(nodes[i + 2]); // mid node next stage
                                                   // (always exists);
                } else if (i < 3) {
                    node.createLink(nodes[i - 1]); // mid node this stage
                } else if (i > nodeCount - 4) {
                    node.createLink(nodes[i + 2]); // mid node next stage
                                                   // (always exists);
                }

                if (i < nodeCount - 3) {
                    node.createLink(nodes[i + 3]); // upper node next stage
                }
                if (i > 3) {
                    node.createLink(nodes[i - 3]); // upper node previous stage
                }
            } else {
                // A lower node!
                if (type != MESH_FWD) {
                    node.createLink(nodes[i - 2]); // mid node this stage

                    if (type == MESH_ALL)
                        node.createLink(nodes[i - 1]); // upper node this stage

                    node.createLink(nodes[i + 1]); // mid node next stage
                                                   // (always exists);
                } else if (i < 3) {
                    node.createLink(nodes[i - 2]); // mid node this stage
                } else if (i > nodeCount - 3) {
                    node.createLink(nodes[i + 1]); // mid node next stage
                                                   // (always exists);
                }

                if (i < nodeCount - 4) {
                    node.createLink(nodes[i + 3]); // lower node next stage
                }
                if (i > 3) {
                    node.createLink(nodes[i - 3]); // lower node previous stage
                }
            }
            // Node[] neighbors = node.getNeighbors();
            // System.out.println("Setting " + neighbors.length +
            // " neighbors for node "
            // + node.getNodeID() + ": {"
            // + getNodeList(neighbors) + "}");
        }

        addLinks(sim, nodes);

        return nodes;
    }

    public static String getNodeList(Node[] nodes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0, n = nodes.length; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("" + nodes[i].getNodeID());
        }
        return sb.toString();
    }

    public static void makeImportantLinks(Simulator sim, Node[] nodes, Node sink) {
        // kör en flood från SINK => meddelanden till alla noder i
        // sink neightbor + skapa path (Hashtabell med alla som msg passerat)

        Hashtable<String,String> path = new Hashtable<String,String>();
        // Add sink node to the "Path" and then go!
        path.put("" + sink.getNodeID(), "sink");

        // For each link, send a message to dest node iff the incoming message
        // have not passed me before (e.g. do not allow cycles)
        // This method will "color" all links so that we know how many unique
        // paths there are?
        traverseLinks(sink, path);

        if (DEBUG) {
            for (int i = 0, n = nodes.length; i < n; i++) {
                printLinks(nodes[i]);
            }
        }

        // Identify important links!
        // e.g. nodes that have only one link with quality 1 will assign
        // important link to that node! - but all link go from a node... so
        // we have to track this with a hashtable...
        Hashtable<String,int[]> nodeCount = new Hashtable<String,int[]>();
        Hashtable<String,Node> nodeTable = new Hashtable<String,Node>();
        Hashtable<String,Node> srcTable = new Hashtable<String,Node>();
        for (int i = 0, n = nodes.length; i < n; i++) {
            Link[] links = nodes[i].getLinks();
            for (int j = 0, m = links.length; j < m; j++) {
                if (links[j].getQuality() != 0) {
                    String node = "" + links[j].getDestination().getNodeID();
                    int[] nc = nodeCount.get(node);
                    if (nc == null) {
                        nc = new int[1];
                        nodeCount.put(node, nc);
                        nodeTable.put(node, links[j].getDestination());
                        srcTable.put(node, nodes[i]);
                    }
                    nc[0]++;
                }
            }
        }
        for (Enumeration<String> e = nodeCount.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            int[] nc = nodeCount.get(key);
            if (nc[0] == 1) {
                Node node = nodeTable.get(key);
                Node src = srcTable.get(key);
                // System.out.println("Node: " + node.getNodeID() +
                // " has an important incoming link from " +
                // src.getNodeID());
                if (src instanceof ExtendedNode) {
                    System.out.println("   => increasing number of important links on: " + src.getNodeID());
                    ((ExtendedNode) src).addImportantLink(node);
                }
            }
        }
        updateImportantLinks(sim, nodes);
    }

    public static void updateImportantLinks(Simulator sim, Node[] nodes) {
        if (sim.visualizer != null) {
            for (int i = 0, n = nodes.length; i < n; i++) {
                Link[] neighbors = nodes[i].getLinks();
                if (neighbors != null) {
                    for (int j = 0, m = neighbors.length; j < m; j++) {
                        Link link = neighbors[j];
                        if ((link instanceof ExtendedLink) && ((ExtendedLink) link).isImportant()) {
                            sim.visualizer.setLinkProperty(link.getSource().getNodeID(), link.getDestination()
                                    .getNodeID(), "color", "#ff0000");
                        }
                    }
                }
            }
        }
    }

    public static void printLinks(Node node) {
        Link[] links = node.getLinks();
        System.out.print("Node " + node.getNodeID() + " => ");
        for (int i = 0, n = links.length; i < n; i++) {
            System.out.print(" " + links[i].getDestination().getNodeID() + "(" + links[i].getQuality() + ")");
        }
        System.out.println();
    }

    public static void traverseLinks(Node start, Hashtable<String,String> path) {
        Link[] links = start.getLinks();
        for (int i = 0, n = links.length; i < n; i++) {
            Node node = links[i].getDestination();
            String nid = "" + node.getNodeID();
            // Next node is not already in path (e.g. no cycle), and I have not
            // sent
            // to next node before using this link
            if (path.get(nid) == null && links[i].getQuality() == 0) {
                // This link was used!
                links[i].setQuality(links[i].getQuality() + 1);
                Hashtable<String,String> newPath = new Hashtable<String,String>(path);
                // Add the new node!
                newPath.put(nid, nid);
                traverseLinks(node, newPath);
            }
            // else Not new to that node - ignore...
        }
    }

}
