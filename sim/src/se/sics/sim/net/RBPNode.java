package se.sics.sim.net;

import se.sics.sim.core.Link;
import se.sics.sim.core.Message;
import se.sics.sim.core.Node;

/**
 * RBP Node
 *
 * simulates an RBP (reliable broadcast propagation) node.
 *
 * Created: Tue Apr 24 08:21:27 2007
 *
 * @version 1.0
 */
public class RBPNode extends ExtendedNode {

    public static final boolean DEBUG = false;

    private int[] lastMessage;
    private int neighborCount = 0;

    private int ackCount;
    private int lastRecvID = 0;

    private int maxResends = 4;
    private double threshold = 1.0;

    // Set the neighbors
    protected void init() {
        super.init();

        Link[] neighbors = getLinks();

        // This should also set number of important neighbors!
        // or should we infer that automatically or get it by communication?
        neighborCount = neighbors == null ? 0 : neighbors.length;

        // Create an empty array of message ID's to store which message
        // each neighbor have seen/sent last
        lastMessage = new int[neighborCount];

        // Note: the paper says four to six, and eight or more... where is the
        // 7?
        // These are fixed in RBP!
        if (neighborCount >= 4 && neighborCount <= 7) {
            threshold = 0.66;
            maxResends = 2;
        } else if (neighborCount >= 8) {
            threshold = 0.5;
            maxResends = 1;
        } else {
            threshold = 1.0;
            maxResends = 3;
        }

        if (simulator.visualizer != null) {
            if (importantCount > 0) {
                simulator.visualizer.setNodeProperty(nodeID, "Important Links", "" + importantCount);
            }
            simulator.visualizer.setNodeProperty(nodeID, "Threshold", "" + (int) (threshold * 100 + 0.5) + '%');
            simulator.visualizer.setNodeProperty(nodeID, "Max Resends", "" + maxResends);
        }
    }

    public void messageReceived(long time, Message message) {
        // System.out.println("Node " + nodeID +
        // ": message received from node: " +
        // src.nodeID + " packetId = " + packetId + " myID: " + lastID);
        MessageNode src = (MessageNode) message.getSource();
        int id = message.getID();
        int type = message.getType();
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
        int index = indexOf(neighbors, src);
        if (index < 0) {
            // Not a neighbor: should never happen with bi-directional links
        } else if (lastMessage[index] < id) {
            lastMessage[index] = id;
            ackCount++;
        } else {
            // Might be a resend, send an ack!
            // Can we avoid sendin acks to nodes that we for example
            // have sent this message to? But what if they got it via another
            // way and believe that we are the one node that has not gotten
            // the message?
            if (type == TYPE_MESSAGE) {
                sendAck(time, src, id);
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
        // IF less than threshold have acked => resend! || numImportant > 0
        if ((ackCount < threshold * neighborCount) && evt.transmitts <= maxResends) {
            return MSG_SEND;
        }
        // Note: although the paper says "if any important links &&
        // resends < 4; transmit" in Figure 3, the text says "up to 4
        // retries when that downstream node does not ack".
        if (importantCount > 0 && evt.transmitts <= 4) {
            int id = evt.getID();
            for (int i = 0; i < importantCount; i++) {
                if (lastMessage[importantLinks[i]] < id) {
                    // The target for this important link has not yet
                    // acknowledged the message
                    return MSG_SEND;
                }
            }
            // All important links have acknowledged the message
        }
        return MSG_CANCEL;
    }

    public BroadcastEvent sendBroadcast(long time, int type, int id) {
        // Hack to get the triggering to make the triggering node to understand
        // that is was sending the first message and this have no acks when it
        if (id > lastSentID)
            ackCount = 0;

        BroadcastEvent evt = super.sendBroadcast(time, type, id);
        evt.setMaxRetransmissions(importantCount > 0 ? 4 : maxResends);
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
