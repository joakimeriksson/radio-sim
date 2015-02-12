package se.sics.sim.net;

import se.sics.sim.core.Link;
import se.sics.sim.core.Message;
import se.sics.sim.core.Node;
import se.sics.sim.core.Simulator;
import se.sics.sim.interfaces.MessageToPacketLayer;
import se.sics.sim.interfaces.RadioNodeInterface;
import com.botbox.scheduler.TimeEvent;

public class TransmissionEvent extends TimeEvent implements Message {

    public static final int START = 0;
    public static final int END = 1;

    // (Now) Public for fast access
    public int packetId;
    public int messageId;
    public int type;
    public int contentType;
    public int transmissionType = 0;
    public int size = 0;
    public int dstID = 0;

    private Link[] dsts;
    private Node src;
    private Simulator sim;

    public TransmissionEvent(int contentType, int macType, int messageId, int packetId, int size, Node src,
            Link[] dest, int dstID, long time, Simulator s) {
        super(time);

        this.src = src;
        dsts = dest;
        sim = s;
        this.contentType = contentType;
        this.type = macType;
        this.messageId = messageId;
        this.packetId = packetId;
        this.size = size;
        this.dstID = dstID;
    }

    // TODO: need to add collission checking here somehow!!!
    public void execute(long time) {
        for (int i = 0, n = dsts.length; i < n; i++) {
            Node destination = dsts[i].destination;
            destination.messageReceived(time, this);
        }
        if (transmissionType == START) {
            /* Schedule again if this was a START event */
            transmissionType = END;
            /*
             * just a random value for now - this should be corrected for CC2420
             * compliance later...
             */
            this.time = time + RadioNodeInterface.TRANSMIT_DELAY
                    + ((size + MessageToPacketLayer.PROTOCOL_OVERHEAD) * RadioNodeInterface.MICROSEC_PER_BYTE_SENDING); // 32
                                                                                                                        // micros
                                                                                                                        // per
                                                                                                                        // byte?
            // System.err.println("Adding send-delay: " + (INITIAL_SEND_DELAY +
            // size * RadioNodeInterface.MICROSEC_PER_BYTE) +
            // " Resulting time is " + this.time);
            sim.addEvent(this);
        } else {
            // TODO
            if (src instanceof RadioNodeInterface) {
                ((RadioNodeInterface) src).transmissionComplete(time, type);
            } else {
                ((RadioNode) src).transmissionComplete(time);
            }
        }
    }

    public int getID() {
        return packetId;
    }

    public Node getSource() {
        return src;
    }

    public int getType() {
        return contentType;
    }

    public int getContentType() {
        return contentType;
    }
}
