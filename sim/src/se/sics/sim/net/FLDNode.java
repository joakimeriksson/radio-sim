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
public class FLDNode extends MessageNode {

    private int lastRecvID = 0;

    public BroadcastEvent sendBroadcast(long time, int type, int id) {
        BroadcastEvent evt = super.sendBroadcast(time, type, id);
        evt.configRetransmits(0, 0);
        return evt;
    }

    public void messageReceived(long time, Message message) {
        int id = message.getID();
        if (id > lastRecvID) {
            lastRecvID = id;
            msgRecv++;
        }
        if (id > lastSentID) {
            sendBroadcast(time, TYPE_MESSAGE, id);
        }
    }
}
