package se.sics.sim.core;

import java.util.HashMap;
import se.sics.sim.interfaces.ApplicationLayer;
import se.sics.sim.interfaces.MacLayer;
import se.sics.sim.interfaces.MessageToPacketLayer;
import se.sics.sim.interfaces.RadioNodeInterface;
import se.sics.sim.interfaces.StateListener;
import se.sics.sim.util.ArrayUtils;

/**
 * A node in the simulation framework
 *
 * The node typically represents the hardware of the node.
 *
 * Created: Tue Apr 24 08:09:56 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public abstract class Node {

    public enum State {

        MAC_OFF_TIME, MAC_ON_TIME, BULK, BETWEEN_PACKET_DELAY;

    };

    public enum Utility {

        TIME, ENERGY, TIME_AND_ENERGY;

    };

    public enum PolicyMode {

        HARDCODED, RANDOM_MOVES, RANDOM_POLICY;

    };

    protected final Utility UTILITY_FUNCTION = Utility.TIME_AND_ENERGY;
    public final int nodeID;

    protected Simulator simulator;
    boolean initialized = false;

    /*
     * The stack - is now here, can be discussed where it should be...
     */
    // protected State state;
    protected MacLayer macLayer;
    protected MessageToPacketLayer messageToPacketLayer;
    protected ApplicationLayer applicationLayer;
    protected StateListener stateListener;
    private Link[] links;
    private Node[] neighbors;

    public long bytesSent = 0;
    public int msgRecv = 0;
    public int msgSent = 0;
    public long radioOffTime;
    // public long sendTime = 0;

    HashMap<State,Integer> state;

    public int preambleCount;
    public int preambleAccCount;
    public int packetsSentCount = 0;
    public int packetsReceivedCount = 0;
    public int messageAccCount = 0;

    private static int globID = 0;

    public Node() {
        this.nodeID = globID++;
        this.state = new HashMap<State,Integer>();
    }

    public int getNodeID() {
        return nodeID;
    }

    public Simulator getSimulator() {
        return simulator;
    }

    public final void init(Simulator simulator) {
        this.simulator = simulator;
        // Activate bottom up
        if (macLayer != null) {
            macLayer.init(this);
            if (this instanceof RadioNodeInterface) {
                ((RadioNodeInterface) this).addPacketReceiver(macLayer);
            }
        }
        if (applicationLayer != null) {
            applicationLayer.init(this);
        }
        init();
        initialized = true;
    }

    protected void init() {
    }

    protected boolean finish() {
        return false;
    }

    // -------------------------------------------------------------------
    // Link handling
    // -------------------------------------------------------------------

    private static int linkCounter = 0;

    protected synchronized static String createLinkID() {
        return "L" + (++linkCounter);
    }

    public Link getLink(Node destination) {
        Link[] links = this.links;
        if (links != null) {
            for (int i = 0, n = links.length; i < n; i++) {
                if (links[i].destination == destination) {
                    return links[i];
                }
            }
        }
        return null;
    }

    public Link getLink(String id) {
        Link[] links = this.links;
        if (links != null) {
            for (int i = 0, n = links.length; i < n; i++) {
                if (links[i].id.equals(id)) {
                    return links[i];
                }
            }
        }
        return null;
    }

    public Link[] getLinks() {
        return links;
    }

    public Link createLink(Node destination) {
        return createLink(destination, null);
    }

    // Assume links will be added/removed much less than accessed
    public synchronized Link createLink(Node destination, String id) {
        Link link = id == null ? getLink(destination) : getLink(id);
        if (link == null) {
            link = createLinkToNode(id == null ? createLinkID() : id, destination);
            links = ArrayUtils.add(Link.class, links, link);
            neighbors = ArrayUtils.add(Node.class, neighbors, destination);
        }
        return link;
    }

    protected Link createLinkToNode(String id, Node destination) {
        return new Link(id, this, destination);
    }

    public synchronized void addLink(Link link) {
        links = ArrayUtils.add(Link.class, links, link);
        neighbors = ArrayUtils.add(Node.class, neighbors, link.destination);
    }

    public synchronized void removeLink(Link link) {
        int index = ArrayUtils.indexOf(links, link);
        if (index >= 0) {
            links = (Link[]) ArrayUtils.remove(links, index);
            neighbors = (Node[]) ArrayUtils.remove(neighbors, index);
        }
    }

    public Node[] getNeighbors() {
        return neighbors;
    }

    // -------------------------------------------------------------------
    // Packet handling
    // -------------------------------------------------------------------

    // Will switch to:
    // public abstract void packetReceived(long time, Packet packet);
    // will become obsolete:
    public abstract void messageReceived(long time, Message message);

    // -------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------
    public void setProperty(State state, int value) {

        this.state.put(state, value);
        if (initialized) {
            switch (state) {
            case BULK:
                stateListener.stateChanged(this, state, value, simulator.getSimulationTime());
                break;
            case MAC_OFF_TIME:
                macLayer.setProperty(state, value);
                stateListener.stateChanged(this, state, value, simulator.getSimulationTime());
                break;
            case MAC_ON_TIME:
                macLayer.setProperty(state, value);
                stateListener.stateChanged(this, state, value, simulator.getSimulationTime());
                break;
            case BETWEEN_PACKET_DELAY:
                applicationLayer.setProperty(state, value);
                stateListener.stateChanged(this, state, value, simulator.getSimulationTime());
                break;
            default:
                System.err.println("Not implemented");
                break;

            }

        } else {
            // System.out.println("At the time when " + nodeID + " changes " +
            // state + " I wasnt initialized!");
        }
    }

    public int getProperty(State state) {
        return this.state.get(state);
    }

    // -------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------

    public static void resetGlobalIDCounter() {
        globID = 0;
    }

    public MacLayer getMacLayer() {
        return macLayer;
    }

    public void setMacLayer(MacLayer macLayer) {
        this.macLayer = macLayer;
    }

    public ApplicationLayer getApplicationLayer() {
        return applicationLayer;
    }

    public void setApplicationLayer(ApplicationLayer applicationLayer) {
        this.applicationLayer = applicationLayer;
        // TODO: right now they are the same
        this.messageToPacketLayer = (MessageToPacketLayer) applicationLayer;
    }

    public void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    public StateListener getStateListener() {
        return stateListener;
    }
}
