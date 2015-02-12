package se.sics.sim.net;

import se.sics.sim.core.Link;
import se.sics.sim.core.Node;

/**
 * Describe class MessageNode here.
 *
 *
 * Created: Tue Apr 24 17:11:47 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public abstract class MessageNode extends Node {

    public static final boolean DEBUG = false; // true;

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_ACK = 1;
    public static double ERROR_RATE = 0.11;
    public static final double CORRELATION_ERROR_RATE = 0.0;

    public static final int MSG_SEND = 1;
    public static final int MSG_SKIP = 2;
    public static final int MSG_CANCEL = 3;

    // MESSAGE, ACK sizes
    public static int[] MESSAGE_SIZE = { 80, 20 };

    protected int lastSentID = 0;

    /**
     * Creates a new <code>MessageNode</code> instance.
     *
     */
    public MessageNode() {
        super();
    }

    protected void init() {
        super.init();

        if (simulator.visualizer != null) {
            Node[] neighbors = getNeighbors();
            if (neighbors != null) {
                simulator.visualizer.setNodeProperty(nodeID, "Neighbors", "" + neighbors.length);
            }
        }
    }

    protected boolean finish() {
        super.finish();
        System.out.printf("Node%3d:   Sent:%8d   Recv:%8d   Bytes Sent:%10d\n", nodeID, msgSent, msgRecv, bytesSent);
        // System.out.println("Node " + nodeID + "\t: sent " + msgSent
        // + " \trecv " + msgRecv
        // + " \tbytes sent: " + bytesSent);
        // System.out.println("Messages sent: " + msgSent);
        // System.out.println("Messages received: " + msgRecv);
        // System.out.println("Bytes sent: " + bytesSent);
        // System.out.println("------------------------------");
        return false;
    }

    // No retransmits of broadcast by default...
    public int transmit(BroadcastEvent evt) {
        // Cancel this broadcast event if sent at least once!
        return evt.transmitts == 0 ? MSG_SEND : MSG_CANCEL;
    }

    public void sendMessage(double errorRate, Link dst, long time, int type, int id) {
        lastSentID = id;
        msgSent++;
        if (errorRate < Math.random()) {
            simulator.addEvent(new MessageEvent(this, dst, time, type, id));
        }
    }

    public BroadcastEvent sendBroadcast(long time, int type, int id) {
        lastSentID = id;
        BroadcastEvent evt = new BroadcastEvent(simulator, this, getLinks(), time + 1, type, id);
        simulator.addEvent(evt);
        return evt;
    }
}
