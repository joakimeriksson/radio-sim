package se.sics.sim.net;

import se.sics.sim.core.*;

/**
 * Describe class FLDNode here.
 *
 *
 * Created: Tue Apr 24 08:21:27 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class TRBNode extends MessageNode {

    public static final boolean DEBUG = false;
    private int[] lastMessage;
    private int neighborCount;
    private int ackCount;
    private int lastRecvID = 0;

    // Set the neighbors
    protected void init() {
        super.init();

        Node[] nodes = getNeighbors();
        neighborCount = nodes == null ? 0 : nodes.length;
        // Create an empty array of message ID's to store which message
        // each neighbor have seen/sent last
        lastMessage = new int[neighborCount];
    }

    public void messageReceived(long time, Message message) {
        // System.out.println("Node " + nodeID +
        // ": message received from node: " +
        // src.nodeID + " packetId = " + packetId + " myID: " + lastID);
        MessageNode src = (MessageNode) message.getSource();
        int type = message.getType();
        int id = message.getID();
        boolean send = false;
        if (lastRecvID < id) {
            msgRecv++;
            lastRecvID = id;
        }
        if (type == TYPE_MESSAGE && id > lastSentID) {
            // Only broadcast once!!!
            lastSentID = id;
            ackCount = 0;
            send = true;
        }

        // Increase the ack on the neighbors if this had not been
        // received or seen by this neighbor before!
        Node[] neighbors = getNeighbors();
        for (int i = 0; i < neighborCount; i++) {
            if (neighbors[i] == src) {
                if (lastMessage[i] < id) {
                    lastMessage[i] = id;
                    ackCount++;
                } else {
                    // Might be a resend, send an ack!
                    // Can we avoid sendin acks to nodes that we for example
                    // have sent this message to? But what if they got it via
                    // another
                    // way and believe that we are the one node that has not
                    // gotten
                    // the message?
                    if (type == TYPE_MESSAGE)
                        sendAck(time, src, id);
                }
                break;
            }
        }

        if (send) {
            if (ackCount < neighborCount) {
                sendBroadcast(time, TYPE_MESSAGE, id);
            } else {
                // Last node does only need to send an ack...
                sendAck(time, src, id);
            }
        }
    }

    // If too few neighbors have acked this message, retransmit!!
    public int transmit(BroadcastEvent evt) {
        return ackCount < neighborCount ? MSG_SEND : MSG_CANCEL;
    }

    public BroadcastEvent sendBroadcast(long time, int type, int id) {
        // Hack to get the triggering to make the triggering node to understand
        // that is was sending the first message and this have no acks when it
        if (id > lastSentID)
            ackCount = 0;
        BroadcastEvent evt = super.sendBroadcast(time, type, id);
        evt.setMaxRetransmissions(4);
        return evt;
    }

    public void sendAck(long time, Node dst, int id) {
        // An ack is also a packet... but only for one node...
        if (DEBUG)
            System.out.println(" -- Node: " + nodeID + " sending ack to " + dst.nodeID);
        Link link = getLink(dst);
        if (link != null) {
            sendMessage(ERROR_RATE, link, time + 1, TYPE_ACK, id);
        }
    }
}
