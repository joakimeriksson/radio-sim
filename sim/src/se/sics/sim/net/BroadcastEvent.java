package se.sics.sim.net;

import se.sics.sim.core.*;
import com.botbox.scheduler.*;

/**
 * Describe class BroadcastEvent here.
 *
 *
 * Created: Wed Apr 25 14:33:04 2007
 *
 * @author <a href="mailto:joakime@GRAYLING"></a>
 * @version 1.0
 */
public class BroadcastEvent extends TimeEvent implements TimerEventListener, Message {

    private Link[] dsts;
    private MessageNode src;
    private Simulator sim;

    int id;
    int type;

    int transmitts = 0;
    int transmitcycles = 0;

    int maxRetransmitts = 4;
    int retransmitDelay = 10;
    int retransmitJitter = 5;

    /**
     * Creates a new <code>BroadcastEvent</code> instance.
     *
     */
    public BroadcastEvent(Simulator sim, MessageNode src, Link[] dsts, long time, int type, int id) {
        super(time);
        this.sim = sim;
        this.src = src;
        this.dsts = dsts;
        this.id = id;
    }

    public Node getSource() {
        return src;
    }

    public int getID() {
        return id;
    }

    public int getType() {
        return type;
    }

    public int getMaxRetransmissions() {
        return maxRetransmitts;
    }

    public void setMaxRetransmissions(int maxRetransmitts) {
        this.maxRetransmitts = maxRetransmitts;
    }

    public int getRetransmitDelay() {
        return retransmitDelay;
    }

    public void setRetransmitDelay(int retransmitDelay) {
        this.retransmitDelay = retransmitDelay;
    }

    // Maybe have a callback mode to the MessageNode for configuration /
    // behavior instead!?
    public void configRetransmits(int r, int delay) {
        maxRetransmitts = r;
        retransmitDelay = delay;
    }

    public void timerEvent(long time, Object data) {
        this.time = time;
        execute(time);
    }

    public void execute(long time) {
        transmitcycles++;
        int trMode = src.transmit(this);
        if (trMode == MessageNode.MSG_SEND) {
            transmitts++;

            // Collect some stats
            src.bytesSent += MessageNode.MESSAGE_SIZE[type];
            src.msgSent++;

            if (MessageNode.DEBUG)
                System.out.print("Node " + src.nodeID + " sends bc to : ");

            if (sim.visualizer != null) {
                // Should be a delay between sending and receiveing!!! e.g.
                // we should instead of calling immediately below create a
                // reception
                // event.
                sim.visualizer.messageSent(src.nodeID);
            }

            if (dsts != null
                    && (MessageNode.CORRELATION_ERROR_RATE <= 0.0 || MessageNode.CORRELATION_ERROR_RATE < Math.random())) {
                for (int i = 0, n = dsts.length; i < n; i++) {
                    Node destination = dsts[i].destination;
                    if (MessageNode.ERROR_RATE < Math.random()) {
                        if (MessageNode.DEBUG)
                            System.out.print(" *" + destination.nodeID + "*");
                        destination.messageReceived(time, this);
                        if (sim.visualizer != null) {
                            sim.visualizer.messageReceived(destination.nodeID);
                        }
                    } else {
                        if (MessageNode.DEBUG)
                            System.out.print(" (" + destination.nodeID + ")");
                    }
                }
            }
        }

        if (MessageNode.DEBUG)
            System.out.println("\t transmits: " + transmitts + " trcyc: " + transmitcycles);
        /* Should we have more retransmits? */
        if (trMode != MessageNode.MSG_CANCEL && transmitcycles <= maxRetransmitts) {
            // Time to resend if not all have acked last flood!!!
            sim.addEvent(new TimerEvent(this, time + retransmitDelay + (int) (Math.random() * retransmitJitter)));
        }
    }

    @Override
    public int getContentType() {
        // TODO Auto-generated method stub
        return 0;
    }

}
