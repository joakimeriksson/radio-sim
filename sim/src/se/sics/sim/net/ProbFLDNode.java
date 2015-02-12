package se.sics.sim.net;

import se.sics.sim.core.*;

/**
 *
 * Created: Tue Apr 24 08:21:27 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class ProbFLDNode extends MessageNode {

    public static final double[] PROBABILITY = new double[] { 1.0, 1.0, 0.9, 0.8, 0.6, 0.5 };

    private double sendProb = 1.0;
    private int lastRecvID = 0;

    protected void init() {
        super.init();

        Node[] nodes = getNeighbors();
        if (nodes == null || nodes.length == 0) {
            return;
        }
        int index = nodes.length - 1;
        if (index >= PROBABILITY.length) {
            index = PROBABILITY.length - 1;
        }
        sendProb = PROBABILITY[index];
        System.out.println("> Node " + nodeID + " set prob to " + sendProb + "  (" + nodes.length + " neighbors)");
    }

    public void messageReceived(long time, Message message) {
        int id = message.getID();
        if (id > lastRecvID) {
            lastRecvID = id;
            msgRecv++;
        }

        // Only broadcast each message at most once!!!
        if (id > lastSentID) {
            if (Math.random() < sendProb) {
                sendBroadcast(time, TYPE_MESSAGE, id);
            }
        }
    }
}
