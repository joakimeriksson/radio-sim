package se.sics.sim.protocols;

import java.util.Random;
import se.sics.sim.core.Message;
import se.sics.sim.core.Node;
import se.sics.sim.core.Node.State;
import se.sics.sim.core.Packet;
import se.sics.sim.core.Simulator;
import se.sics.sim.interfaces.MacLayer;
import se.sics.sim.interfaces.MessageToPacketLayer;
import se.sics.sim.interfaces.PacketReceiver;
import se.sics.sim.interfaces.RadioModes;
import se.sics.sim.interfaces.RadioNodeInterface;
import se.sics.sim.net.TransmissionEvent;
import com.botbox.scheduler.TimeEvent;

public class XMAC implements MacLayer, PacketReceiver {

    // TODO:
    // some form of getters / setters for the xmac on/off-settings

    // be STROBE_INTERVAL_DEFAULT

    // public static final long LISTEN_TIME_DEFAULT = 10000;
    // public static final long SLEEP_TIME_DEFAULT = 90000;
    // public static final long PREAMBLE_TIME = 8000;

    public static final long RADIO_OFF_AFTER_BULK_DELAY = 10;

    long listenTime = LISTEN_TIME_DEFAULT;
    long sleepTime = SLEEP_TIME_DEFAULT;
    private long strobeInterval = PREAMBLE_TIME;

    public static final int PREAMBLE_COUNT = 35; // (int)(SLEEP_TIME_DEFAULT /
    private int debug = 1; // 2;

    private int radioMode;
    private boolean policySaysRadioOn = false;
    private boolean policySaysRadioOff = false;

    private long lastTime;
    int preMode = 0;
    private XMACSender xmacSender;
    private XMACSender xmacAccSender;

    private PacketReceiver application;
    private Random myRandom;
    private int demandPacketAcc = 0;
    private int waitingForPacketAcc = 0;
    private boolean waitingForPacket = false;
    private boolean offAfterSendingPacketAcc = false;
    private boolean offAfterReceivingPacketAcc = false;

    public long accTime[] = new long[RadioModes.MODE_MAX + 1];

    private boolean transmit = false;
    private boolean xmacSending = false;

    RadioNodeInterface radioNodeInterface;
    Node node;
    Simulator simulator;
    private XMACOnOff xmacOnOff;
    private boolean finishedWithoutErrors = true;

    private void handlePacket(TransmissionEvent te) {

        if (te.dstID != node.nodeID) {
            if (1 < debug) {
                System.out.println(node.nodeID + "  packet ignored: for: " + te.dstID);
            }
            return;
        }
        switch (te.type) {
        case PREAMBLE:
            if (1 < debug) {
                System.out.println(node.nodeID + "  *** PREAMBLE received " + te.getTime());
            }
            /* check if it is to me ??? */

            xmacAccSender.sendPreambleAck(te.getSource().nodeID, te.packetId);
            waitingForPacket = true;
            break;
        case PREAMBLE_ACK:
            if (1 < debug) {
                System.out.println(node.nodeID + "  *** PREAMLBE ACK received " + te.getTime());
            }
            xmacSender.preambleAckReceived(te.getID());
            break;

        case PACKET_ACK:
            if (1 < debug) {
                System.out.println(node.nodeID + "  *** PACKET ACK received " + te.getTime());
            }
            xmacSender.packetAckReceived(te.getID(), te.getTime());
            break;

        case PACKET:

            waitingForPacket = false;
            node.packetsReceivedCount++;
            if (1 < debug) {
                System.out.println(node.nodeID + "  *** PACKET received from: " + te.getSource().nodeID + " type: "
                        + te.type + " with packetId " + te.packetId + " time " + te.getTime());
            }
            if (demandPacketAcc == LINK_ACC_BEFORE_FORWARD) {
                xmacAccSender.sendPacketAck(te.getSource().nodeID, te.packetId);
                if (1 < debug) {
                    System.out.println(node.nodeID + " sends acc immeditiately");
                }

            }
            // Always hand it to the application!
            application.receivePacket(te);

            break;
        }
    }

    private void changeMode(int newMode, long time) {
        // System.out.println(node.nodeID + " Changing mode to: " +
        // RadioModes.MODE_NAME[newMode]
        // + " time: " + time);
        accTime[radioMode] += time - lastTime;
        lastTime = time;
        radioMode = newMode;
        if (radioMode == RadioModes.MODE_LISTEN) {
            // turn radio on!
            if (1 < debug) {
                System.out.println(node.nodeID + " is turning radio on at time " + time);
            }
            radioNodeInterface.on(time);
        } else if (radioMode == RadioModes.MODE_TRANSMIT) {
            // nuffin?
        } else {
            // turn off radio!
            if (1 < debug) // if(1 < debug)
            {
                System.out.println(node.nodeID + " is turning radio off at time " + time);
            }
            radioNodeInterface.off(time);
        }
    }

    private void transmitPacket(int contentType, int macType, int dstID, int messageId, int packetId, int size) {
        /*
         * here we invoke the radio-layer which will schedule a transmission
         * event
         */

        long time = simulator.getSimulationTime();
        transmit = true;
        preMode = radioMode;
        changeMode(RadioModes.MODE_TRANSMIT, time);
        time += RadioNodeInterface.TRANSMIT_STARTUP_DELAY;
        /* add some time for starting up transmission ??? */

        node.bytesSent += size;
        radioNodeInterface.send(new TransmissionEvent(contentType, macType, messageId, packetId, size, node, node
                .getLinks(), dstID, time, simulator));

    }

    @Override
    public void addPacketReceiver(PacketReceiver packetReceiver) {
        application = packetReceiver;

    }

    @Override
    public void off() {
        policySaysRadioOff = true;
        if (!transmit && !xmacSending && !radioNodeInterface.isReceiving()) {
            /* only if we are in non transmit mode ... */
            long time = simulator.getSimulationTime();
            System.out.println("Policy says radio off at time " + time);
            changeMode(RadioModes.MODE_OFF, time);
        }
    }

    @Override
    public void on() {
        // System.out.println("VEM!!");
        // try {
        // int error = 1 / 0;
        // } catch(Exception e) {
        // e.printStackTrace();
        // }
        policySaysRadioOn = true;
        if (!transmit && !xmacSending && !radioNodeInterface.isReceiving()) {
            // System.out.println(node.nodeID + " turns radio on. time " +
            // simulator.getSimulationTime());
            /* only if we are in non transmit mode ... */
            long time = simulator.getSimulationTime();
            changeMode(RadioModes.MODE_LISTEN, time);
        }
        // else { System.out.println(node.nodeID +
        // " will soon turn on radio. time " + simulator.getSimulationTime()); }
    }

    // private void

    // @Override
    // public void sendAck(TransmissionEvent transmissionEvent) {
    // System.out.println(node.nodeID + " is sending acc for packet " +
    // transmissionEvent.id);
    // xmacAccSender.sendPacketAck(transmissionEvent.getSource().nodeID,
    // transmissionEvent.id);
    // }

    @Override
    public void send(Packet packet, long time) {
        if (1 < debug) {
            System.out.println(node.nodeID + " start packet-send at time " + time + " for packet with packetId "
                    + packet.packetId);
        }
        radioNodeInterface.startSendingPacket(time);
        xmacSender.sendPacket(simulator, time, packet.contentType, packet.destinationId, packet.messageId,
                packet.packetId, packet.size);
    }

    @Override
    public void send(Packet packet) {
        send(packet, simulator.getSimulationTime());
    }

    @Override
    public void setAccMode(int accMode) {
        demandPacketAcc = accMode;
    }

    @Override
    public void init(Node node) {
        this.node = node;
        this.radioNodeInterface = (RadioNodeInterface) node;
        this.application = node.getApplicationLayer();
        this.simulator = node.getSimulator();
        this.xmacSender = new XMACSender();
        this.xmacAccSender = new XMACSender();

        this.xmacOnOff = new XMACOnOff();

        /*
         * Write initial values on blackboard. Here?
         */
        node.setProperty(Node.State.MAC_OFF_TIME, (int) sleepTime);

        long seed = node.nodeID;
        boolean random = true;
        String rs = (String) simulator.getProperty("system.random");
        if (rs != null) {
            random = 0 < Integer.parseInt(rs);
        }
        if (random) {
            seed = node.nodeID * System.currentTimeMillis();
        }

        this.myRandom = new Random(seed);
        // int start = (int) (myRandom.nextDouble() * 100000);
        int start = (int) (Math.random() * 10000);
        xmacOnOff.setTime(start);
        simulator.addEvent(xmacOnOff);

    }

    /*
     * Helper class(es)
     */
    public class XMACSender extends TimeEvent {

        // static final int SNM = 256;

        int preamblesLeft = PREAMBLE_COUNT;
        int dstID = 0;
        int messageId = 0;
        int packetId = 0; // message packetId

        int size = 0;
        int contentType;
        // public int pushCount = 0;
        int sequenceNumber = 0;

        public XMACSender() {
            super(0);
        }

        public void sendPacket(Simulator sim, long time, int contentType, int dstID, int messageId, int packetId,
                int size) {
            if (this.size == 0) { // time != 0 ||

                this.time = time + RadioNodeInterface.RADIO_INIT_TRANSMIT_TIME;

                if (1 < debug) {
                    System.out.println(node.nodeID + " will be sending packet with seqno " + sequenceNumber
                            + " at time " + this.time);
                }
                this.contentType = contentType;
                this.messageId = messageId;
                this.packetId = packetId;
                this.size = size;
                this.dstID = dstID;
                preamblesLeft = PREAMBLE_COUNT;
                // System.out.println(node.nodeID +
                // ": scheduling sendMessage to be run at " + this.time);
                sim.addEvent(this);
                // can't be done here
                // xmacSending = true;
                // changeMode(RadioModes.MODE_LISTEN, time);
            } else {
                if (0 < debug) {
                    System.out.println(node.nodeID + " is trying to start a new send while already busy, at time "
                            + time + ". ERROR! SequenceNumber is " + sequenceNumber);
                }
                finishedWithoutErrors = false;
                simulator.stop();
            }
        }

        public void execute(long ctime) {
            xmacSending = true;
            changeMode(RadioModes.MODE_LISTEN, ctime);
            execute(ctime, sequenceNumber);
        }

        public void execute(long ctime, int seqNo) {

            if (seqNo != sequenceNumber) {
                // this is an old strobe
                if (1 < debug) {
                    System.out.println(node.nodeID + " OLDSTROBE");
                }
                return;
            }
            // System.out.println(node.nodeID + ": POPPING at time " + ctime);

            if (xmacSending) { // && waitingForPacketAcc == 0) {
                node.preambleCount++;
                transmitPacket(0, PREAMBLE, dstID, messageId, packetId, RadioNodeInterface.PREAMBLE_SIZE);
                if (1 < debug) {
                    System.out.println(node.nodeID + " sending preamble for seqNo " + seqNo + " - left: "
                            + preamblesLeft + " time: " + ctime);
                }
                preamblesLeft--;
                if (preamblesLeft > 0) {
                    time = ctime + getStrobeInterval();
                    // System.out.println(node.nodeID + ": scheduling strobe " +
                    // preamblesLeft + " to be run at " + time + " based on " +
                    // ctime + " " + strobeInterval);
                    simulator.addEvent(new StrobeSender(sequenceNumber, time, this));
                } else {
                    System.out.println(node.nodeID + " giving up on sending message " + messageId);

                    finishedWithoutErrors = false;
                    simulator.stop();
                    // radioNodeInterface.doneSendingPacket();
                    // xmacSending = false;
                    // if (offAfterReceivingPacketAcc) {
                    // offAfterReceivingPacketAcc = false;
                    // changeMode(RadioModes.MODE_OFF, simulator
                    // .getSimulationTime());
                    // }
                    // ((MessageToPacketLayer) application)
                    // .transmissionFailed(time);
                }
            } else {
                System.out.println(node.nodeID
                        + " is trying to start a new send while already busy. ERROR! SequenceNumber is "
                        + sequenceNumber);
                finishedWithoutErrors = false;
                simulator.stop();
            }
            // System.out.println("\n" + node.nodeID +
            // ": XMACSender is DONE at " + ctime);
        }

        public void sendPreambleAck(int dstID, int packetId) {
            node.preambleAccCount++;
            // TODO
            transmitPacket(0, PREAMBLE_ACK, dstID, messageId, packetId, RadioNodeInterface.PREAMBLE_ACK_SIZE);
        }

        public void preambleAckReceived(int packetId) {

            if (this.packetId == packetId) {
                // we need to keep track of to which send-event strobes belong,
                // in order
                // to be able to discard old strobes
                sequenceNumber = (++sequenceNumber) % 256;
                if (contentType == PACKET_ACK) {
                    if (1 < debug) {
                        System.out.println(node.nodeID + " Transmitting packet-ack to " + dstID + " with size " + size
                                + " and packetId " + packetId + " at time " + simulator.getSimulationTime());
                    }
                    transmitPacket(PACKET_ACK, PACKET_ACK, dstID, messageId, packetId, size);

                    // waitingForPacketAcc = demandPacketAcc;

                } else {
                    if (1 < debug) {
                        System.out.println(node.nodeID + " Transmitting packet to " + dstID + " with size " + size
                                + " and packetId " + packetId);
                    }

                    node.packetsSentCount++;
                    transmitPacket(contentType, PACKET, dstID, messageId, packetId, size);
                    waitingForPacketAcc = demandPacketAcc;

                }
                // size = 0;
                // xmacSending = false;
                // if(offAfterReceivingMessageAck) {
                // offAfterReceivingMessageAck = false;
                // changeMode(RadioModes.MODE_OFF,
                // simulator.getSimulationTime());
                // }
                // radioNodeInterface.doneSending();
            }
        }

        public void sendPacketAck(int dstID, int packetId) {
            node.messageAccCount++;
            // FIXME
            transmitPacket(0, PACKET_ACK, dstID, messageId, packetId, RadioNodeInterface.MESSAGE_ACK_SIZE);
        }

        public void packetAckReceived(int packetId, long accTime) {
            if (1 < debug) {
                System.out.println(node.nodeID + " received acc for " + packetId + " at time " + accTime);
            }
            waitingForPacketAcc = 0;
            if (this.packetId == packetId) {
                size = 0;
                xmacSending = false;
                if (offAfterReceivingPacketAcc) {
                    if (1 < debug) {
                        System.out.println(node.nodeID + " radio off");
                    }

                    offAfterReceivingPacketAcc = false;
                    changeMode(RadioModes.MODE_OFF, simulator.getSimulationTime());
                } else {
                    if (1 < debug) {
                        System.out.println(node.nodeID + " is staying in mode " + radioMode);
                    }
                }
                // if(node.nodeID == 0) {
                // System.out.println(packetId + " I SHOULD BE DONE! " +
                // accTime);
                // }
                radioNodeInterface.doneSendingPacket();
                ((MessageToPacketLayer) application).transmissionComplete(accTime);
            } else {
                System.out.println(node.nodeID + " error in receive. got " + packetId + " was expecting "
                        + this.packetId);
                finishedWithoutErrors = false;
                simulator.stop();
            }
        }

    } // end XMACSender-class

    public class StrobeSender extends TimeEvent {

        XMACSender xmacSender;
        int seqNo;

        StrobeSender(int seqNo, long time, XMACSender xmacSender) {
            super(time);
            this.seqNo = seqNo;
            this.xmacSender = xmacSender;
        }

        @Override
        public void execute(long currentTime) {
            xmacSender.execute(currentTime, seqNo);

        }

    }

    // This is used for upcall from radio-layer
    @Override
    public void receivePacket(Message message) {
        handlePacket((TransmissionEvent) message);

    }

    // @Override
    /*
     * public void receivePacket(Packet packet) { // TODO Auto-generated method
     * stub
     * 
     * }
     */
    // This is used for upcall from radio-layer
    @Override
    public void transmissionComplete(long time, int type) {
        transmit = false;
        if (type == PREAMBLE) { // || (type == PACKET && waitingForPacketAcc ==
                                // MacLayer.LINK_ACC_BEFORE_FORWARD)) {
            // System.out.println(node.nodeID
            // + " finished transmission of type " + type +
            // ", so staying online waiting for acc");
            changeMode(RadioModes.MODE_LISTEN, time);
            if (preMode == RadioModes.MODE_OFF || offAfterReceivingPacketAcc) {
                /*
                 * TODO this should be split in two:
                 * "offAfterReceivingPacketAcc" AND
                 * "offAfterGivingUpSendingPrembles"
                 */
                offAfterReceivingPacketAcc = true;
                // if(node.nodeID==0) {//{if(1 < debug) {
                // System.out.println(node.nodeID
                // +
                // " setting/keeping flag for radio off after receiving message acc at "
                // + time);
                // }
            }
        } else if (type == PREAMBLE_ACK) {
            // System.out.println(node.nodeID
            // +
            // " finished transmission of type PREAMBLE_ACK, so staying online waiting for the message");
            changeMode(RadioModes.MODE_LISTEN, time);
            if (preMode == RadioModes.MODE_OFF) {
                offAfterSendingPacketAcc = true;
                if (1 < debug) {
                    System.out.println(node.nodeID + " setting flag for radio off after sending message acc");
                }
            }
        } else if (type == PACKET) {

            if (demandPacketAcc == MacLayer.LINK_ACC_OFF) {
                ((MessageToPacketLayer) application).transmissionComplete(time);

            }

        } else if (type == PACKET_ACK) {
            xmacSending = false;
            if (offAfterSendingPacketAcc) {
                offAfterSendingPacketAcc = false; // offAfterReceivingPacketAcc
                                                  // = false;
                if (1 < debug) {
                    System.out.println(node.nodeID
                            + " finished transmission of type PACKET_ACK, switching to mode OFF at time " + time);
                }
                changeMode(RadioModes.MODE_OFF, time);
            } else {
                if (1 < debug) {
                    System.out.println(node.nodeID + " finished transmission of type " + type
                            + " switching back to mode " + preMode);
                }
                changeMode(preMode, time);
            }
            if (demandPacketAcc == MacLayer.LINK_ACC_AFTER_FORWARD) {
                ((MessageToPacketLayer) application).transmissionComplete(time);

            }
        }
    }

    @Override
    public void setProperty(State state, int value) {
        // TODO
        if (state == State.MAC_OFF_TIME) {
            if (value == 0) {
                on();
            } else if (value == 1) { // reset to default...
                policySaysRadioOn = false;
                sleepTime = SLEEP_TIME_DEFAULT;
                if (1 < debug) {
                    System.out.println(node.nodeID + " returning to non-bulk mode");
                }
            } else {
                policySaysRadioOn = false;
                sleepTime = value;
            }
        } else if (state == State.MAC_ON_TIME) {
            System.out.println(node.nodeID + " doesn't know what to do with MAC_ON_TIME...");
        }

    }

    class XMACOnOff extends TimeEvent {

        public XMACOnOff() {
            super(0);
        }

        public void execute(long currentTime) {

            int nextMode = 0;
            offAfterReceivingPacketAcc = false;
            offAfterSendingPacketAcc = false;
            // if(node.nodeID == 0) System.out.println(node.nodeID +
            // " executing at " + currentTime);
            if (policySaysRadioOn || (radioMode == RadioModes.MODE_OFF && !policySaysRadioOff)) {

                nextMode = RadioModes.MODE_LISTEN;
                time = currentTime + listenTime;
                if (1 < debug)
                    System.out.println(node.nodeID + " scheduling radio off at " + time);

            } else {
                nextMode = RadioModes.MODE_OFF;
                time = currentTime + sleepTime;
            }
            simulator.addEvent(this);
            boolean receiving = waitingForPacket || radioNodeInterface.isReceiving();
            if (!(waitingForPacketAcc == MacLayer.LINK_ACC_BEFORE_FORWARD) && !transmit && !xmacSending && !receiving) {
                /* only if we are in non transmit mode ... */
                if (1 < debug) {
                    System.out.println(node.nodeID + " going to radio mode " + nextMode + " at time " + currentTime);
                }
                changeMode(nextMode, currentTime);
            } else {
                // System.out.println(node.nodeID +
                // " could not change radiomode at " + currentTime);
                if (nextMode == RadioModes.MODE_OFF) {
                    if (1 < debug) {
                        System.out.println(node.nodeID + " setting flag to turn radio off after: " + transmit + " "
                                + xmacSending + " " + receiving);
                    }
                    if (transmit && !xmacSending) {
                        preMode = RadioModes.MODE_OFF;
                    } else if (xmacSending) {
                        offAfterReceivingPacketAcc = true;
                    } else if (!(xmacSending || transmit)) {
                        // == receiving while not being in send-mode
                        offAfterSendingPacketAcc = true;
                    }
                    // offAfterSendingMessageAck = false;
                } else {
                    // offAfterSendingMessageAck = false;
                    if (1 < debug) {
                        System.out.println(node.nodeID + " No need to put radio on - it already is!");
                    }
                }

            }
        }
    }

    @Override
    public boolean finish() {
        return finishedWithoutErrors;

    }

    public void setStrobeInterval(long strobeInterval) {
        this.strobeInterval = strobeInterval;
    }

    @Override
    public long getStrobeInterval() {
        return strobeInterval;
    }

}
