package se.sics.sim.applications;

import java.util.Random;
import se.sics.sim.core.Message;
import se.sics.sim.core.Node;
import se.sics.sim.core.Packet;
import se.sics.sim.core.Simulator;
import se.sics.sim.core.StopEvent;
import se.sics.sim.core.Node.State;
import se.sics.sim.interfaces.ApplicationLayer;
import se.sics.sim.interfaces.MacLayer;
import se.sics.sim.interfaces.MessageToPacketLayer;
import se.sics.sim.interfaces.MessageTypes;
import se.sics.sim.interfaces.RadioNodeInterface;
import se.sics.sim.net.TransmissionEvent;
import com.botbox.scheduler.TimeEvent;

public class BulkSend implements ApplicationLayer, MessageToPacketLayer {

    // public static final long SEND_INTERVAL = 300000;

    // "Send a few KB every sec"

    private static long STOP_TIME;// =
                                  // MessageToPacketLayer.MAX_TIME_TO_TRANSFER_ONE_MESSAGE;
                                  // //= app half of optimal total time to
                                  // send

    // public static final long LESS_THAN_OPTIMAL_DELAY = (long)
    // (MessageToPacketLayer.IDEAL_PACKET_SEND_INTERVAL / 2) + FORWARD_DELAY;
    //
    // public static final long PACKET_SEND_INTERVAL = (long)
    // (MessageToPacketLayer.IDEAL_PACKET_SEND_INTERVAL +
    // LESS_THAN_OPTIMAL_DELAY);

    private long messageSendInterval = 0; // 100*1000*1000; //i microsec.

    private long packetSendInterval = 0; // PACKET_SEND_INTERVAL; //Default
    private int bytesReceived;
    private int remainingBytesToSend;
    private boolean sendingLastPacket;
    private static final int HANDLE_BULK = 40;
    private static final int IDLE = 0;

    // private static final int PPM = 10;

    private Node node; // should be read only...
    private RadioNodeInterface radioNodeInterface;
    private MacLayer packetSendProtocol;
    private Simulator simulator;

    private static boolean sourceIsDone;
    private static boolean sinkIsDone;
    private int receivedMessageCount = 0;
    private int sentMessageCount = 0;
    private int receivedPacketCount;
    private int sentPacketCount;

    private int numberOfTestMessages = 10;
    private int numberOfNodes = 5; // if not specified with
                                   // "network.numberOfNodes"
    // this val is used

    private Random myRandom;
    // TODO
    boolean dummy = false;

    private int bulkMode;
    private int accMode;

    private int currentMessageId;
    // private int currentPacketId;
    private TransmissionEvent packetThatNeedsToBeAcced;
    private int waitingToCompletePacketAcc = -1;
    /*
     * Statistics
     */
    private int errorCount = 0;
    private int bytesLost;
    long lastMessageSendStartTime = 0;
    long activeMessageSendTime = 0;

    private int debug = 0; // 1;

    public void setPacketSendProtocol(MacLayer packetSendProtocol) {
        this.packetSendProtocol = packetSendProtocol;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void init(final Node node) {
        this.node = node;
        // TODO, maybe... - this is ugly:
        this.radioNodeInterface = (RadioNodeInterface) node;
        this.packetSendProtocol = node.getMacLayer();
        this.simulator = node.getSimulator();

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

        /*
         * cannot have randomness in RL-mode (without compensating for the
         * change in max utility...)
         */
        final int start = 50000; // (int) (myRandom.nextDouble() * 100000);

        String notm = (String) node.getSimulator().getProperty("application.numberOfTestMessages");
        if (notm != null) {
            numberOfTestMessages = Integer.parseInt(notm);
        }
        String non = (String) node.getSimulator().getProperty("network.numberOfNodes");
        if (non != null) {
            numberOfNodes = Integer.parseInt(non);
        }

        String accModeString = (String) node.getSimulator().getProperty("bulkSend.accMode");
        if (accModeString != null) {
            this.accMode = Integer.parseInt(accModeString);
        }
        packetSendProtocol.setAccMode(accMode);
        String psi = (String) node.getSimulator().getProperty("bulkSend.packetSendInterval");

        if (psi != null) {
            packetSendInterval = Long.parseLong(psi);
        } else {
            packetSendInterval = (long) (10 * MessageToPacketLayer.IDEAL_PACKET_SEND_INTERVAL);
        }

        if (packetSendInterval == 0) {
            messageSendInterval = (long) (2 * (MessageToPacketLayer.MESSAGE_LENGTH / MessageToPacketLayer.APPLICATION_PACKET_SIZE) * (10 * MessageToPacketLayer.IDEAL_PACKET_SEND_INTERVAL));
        } else {
            messageSendInterval = 10
                    * (MessageToPacketLayer.MESSAGE_LENGTH / MessageToPacketLayer.APPLICATION_PACKET_SIZE)
                    * packetSendInterval;
        }

        // Here we can calc. stoptime!
        String stopTime = (String) node.getSimulator().getProperty("simulation.stopTime");

        if (stopTime != null) {
            STOP_TIME = Long.parseLong(stopTime);
        } else {
            STOP_TIME = 2 * (MESSAGE_LENGTH / APPLICATION_PACKET_SIZE) * packetSendInterval; // +
                                                                                             // 30*numberOfNodes*packetSendInterval;
            System.out.println("Warning, no stopTime defined, using " + STOP_TIME);
        }

        // System.out.println("StopTime will be " + STOP_TIME);
        // System.out.println(node.nodeID + " is setting packetSendInterval to "
        // + packetSendInterval + " and messageSendInterval to " +
        // messageSendInterval);

        /*
         * Write initial values on blackboard. Here?
         */
        node.setProperty(Node.State.BETWEEN_PACKET_DELAY, (int) packetSendInterval);
        // simulator.setProperty("simulation.maxUtility", new Long(STOP_TIME));

        if (node.nodeID == 0) {
            TimeEvent sender = new TimeEvent(0) {
                // int messageId = 0;

                public void execute(long currentTime) {
                    if (sentMessageCount < numberOfTestMessages) {

                        this.time = start;
                        if (!dummy && 1 < debug) {
                            System.out.println("Sending message number " + sentMessageCount + " at time " + start
                                    + " scheduling next message for time NEVUR");
                        }
                        // this.time = currentTime + messageSendInterval;
                        // if(!dummy && 0 < debug) {
                        // System.out.println("Sending message number "
                        // + sentMessageCount + " at time "+ currentTime +
                        // " scheduling next message for time " + this.time);
                        // }

                        // We might want to start handling bulk, _after_ sending
                        // the first packet
                        // (since the first two packets req. longer inbetween
                        // delay anyhow...)
                        bulkMode = HANDLE_BULK;
                        node.setProperty(State.BULK, 1);

                        currentMessageId = (sentMessageCount + 1);
                        remainingBytesToSend = MESSAGE_LENGTH;

                        startSendingMessage();
                        if (!dummy) {
                            dummy = true;
                            packetSendProtocol.send(new Packet(1, currentMessageId, 0, APPLICATION_PACKET_SIZE, node,
                                    MessageTypes.BULK_MESSAGE), currentTime);
                        }

                        // was messageId++, //now do "sentMessageCount++" after
                        // actually sending..
                        // sentMessageCount++;

                        // simulator.addEvent(this);
                    } else {
                        if (1 < debug) {
                            System.out.println("Already done sending required nb of messages");
                        }
                    }
                }
            };
            /* send one packet "immediately" */
            // sender.execute(start);
            simulator.addEvent(sender);
        }
    }

    // @Override
    /*
     * public void receivePacket(Packet packet) { System.err.println(node.nodeID
     * + " says NOT DONE"); }
     */
    @Override
    public void receivePacket(Message message) {

        TransmissionEvent te = (TransmissionEvent) message;
        long startTime = simulator.getSimulationTime() + (te.size * RadioNodeInterface.MICROSEC_PER_BYTE_COPYING);
        // Start handling bulk
        if (bulkMode == IDLE) {
            if (1 < debug) {
                System.out.println(node.nodeID + " is switching to BULK bulkMode");
            }
            bulkMode = HANDLE_BULK;
            node.setProperty(State.BULK, 1);
            startSendingMessage(startTime);
        }

        // bytesReceived += te.size;
        bytesReceived = te.size;
        receivedPacketCount++;
        currentMessageId = te.messageId;
        if (1 < debug) {
            System.out.println(node.nodeID + " received packet no " + receivedPacketCount + " with messageId "
                    + currentMessageId + " and packetId " + te.packetId + " at time " + simulator.getSimulationTime());
        }

        if (te.getContentType() == MessageTypes.LAST_BULK_MESSAGE) { // if(receivedPacketCount
                                                                     // ==
                                                                     // PPM)
                                                                     // {
            sendingLastPacket = true;
            receivedPacketCount = 0;
            receivedMessageCount++;

            if (node.nodeID == numberOfNodes - 1) {
                if (1 < debug) {
                    System.out.println(node.nodeID + " is last in chain and will not forward message with messageId "
                            + te.messageId + " Message-count is " + receivedMessageCount);
                }
                bulkMode = IDLE;
                if (1 < debug) {
                    System.out.println(node.nodeID + " is leaving BULK mode");
                }
                node.setProperty(State.BULK, 0);

                if (receivedMessageCount == numberOfTestMessages) { // last node
                                                                    // in chain
                                                                    // also
                    // decides when the fat lady
                    // has sung
                    if (sourceIsDone) {
                        if (1 < debug) {
                            System.out.println(node.nodeID + " is stopping at time " + STOP_TIME);
                        }
                        simulator.addEvent(new StopEvent(simulator, STOP_TIME));
                    } else {
                        System.out.println("\n" + node.nodeID + " is setting stop flag");
                        sinkIsDone = true;
                    }
                }
            }
        }
        if (node.nodeID < numberOfNodes - 1) {
            // Always try to forward to the next higher neighbour
            remainingBytesToSend = bytesReceived;
            bytesReceived = 0;
            if (1 < debug) {
                System.out.println(node.nodeID + " is forwarding packet with packetId " + te.packetId + " and size "
                        + remainingBytesToSend + " to " + (node.nodeID + 1) + " receivedMessageCount is "
                        + receivedMessageCount + " accMode is " + accMode + " at time " + startTime);
            }
            // if(remainingBytesToSend != MESSAGE_LENGTH) {
            // System.err.println(node.nodeID + " ERROR in transmission");
            // errorCount ++;
            // bytesLost += (MESSAGE_LENGTH - remainingBytesToSend);
            // }

            // save it so we can acc it
            if (accMode == MacLayer.LINK_ACC_AFTER_FORWARD) {
                packetThatNeedsToBeAcced = te;
            }

            packetSendProtocol.send(
                    new Packet(node.nodeID + 1, te.messageId, te.packetId, remainingBytesToSend, te.getSource(), te
                            .getContentType()), startTime);

        } else { // I'm the last node in the forwarding chain, schedule an acc
                 // to be sent after some nice delay...
            // (we want the prev. node to be able to send its acc first...)
            if (accMode == MacLayer.LINK_ACC_AFTER_FORWARD) {
                // (int destinationId, int packetId, int size, Node source, int
                // contentType)
                waitingToCompletePacketAcc = currentMessageId;
                long accTime = simulator.getSimulationTime() + 100000;
                System.out.println(node.nodeID + ": sending ACC for packet " + te.packetId + " at time " + accTime);
                packetSendProtocol.send(new Packet(te.getSource().nodeID, te.messageId, te.packetId,
                        RadioNodeInterface.MESSAGE_ACK_SIZE, node, MacLayer.PACKET_ACK), accTime);
                // te
            }
        }

    }

    @Override
    public void setProperty(State state, int value) {
        if (state == State.BETWEEN_PACKET_DELAY) {
            packetSendInterval = value;
            if (1 < debug) {
                System.out.println(node.nodeID + " changed its packetsend interval to " + value);
            }
        } else {
            System.err.println(node.nodeID + " Bulksend does not know what to do with state " + state);
        }

    }

    @Override
    public void transmissionComplete(long time) {

        if (-1 < waitingToCompletePacketAcc) {
            System.out.println(node.nodeID + " has finished sending message ack for packet "
                    + waitingToCompletePacketAcc); // +
                                                   // ". Remaining bytes to send: "
                                                   // + remainingBytesToSend);
            waitingToCompletePacketAcc = -1;
            return;
        }

        sentPacketCount++;
        String acking = "";
        if (1 < debug) {
            if (accMode == MacLayer.LINK_ACC_OFF) {
                acking = ". Message-accing is off"; // +
                                                    // ". Remaining bytes to send: "
                                                    // + remainingBytesToSend);
            } else if (accMode == MacLayer.LINK_ACC_BEFORE_FORWARD) {
                acking = ". Message-accing is already done"; // +
                                                             // ". Remaining bytes to send: "
                                                             // +
                                                             // remainingBytesToSend);
            } else if (accMode == MacLayer.LINK_ACC_AFTER_FORWARD && 0 < node.nodeID) {
                acking = ". Message-accing needs to be done"; // +
                                                              // ". Remaining bytes to send: "
                                                              // +
                                                              // remainingBytesToSend);
            }
            System.out.println(node.nodeID + " has finished sending packet " + sentPacketCount + " " + acking);
        }
        // if(bulkMode == HANDLE_BULK) { //HANDLE_BULK) {

        if (sendingLastPacket) {
            sendingLastPacket = false;
            doneSendingMessage();
            if (1 < debug) {
                System.out.println(node.nodeID + " is done forwarding message " + currentMessageId + " at time "
                        + simulator.getSimulationTime());
            }
            sentPacketCount = 0;

            sentMessageCount++;

            /*
             * Stop handling bulk - here the policy can make a new decision -
             * which "should" eventually be to turn ordinary xmac on..
             */
            bulkMode = IDLE;
            if (1 < debug) {
                System.out.println(node.nodeID + " is leaving BULK mode");
            }
            node.setProperty(State.BULK, 0);

            if (node.nodeID == 0 && currentMessageId == numberOfTestMessages) {
                if (1 < debug) {
                    System.out.println("#### All packets sent! #### "
                            + (sinkIsDone ? " and sink is done, adding stopEvent at time " + STOP_TIME
                                    : " waiting for sink. "));
                }
                if (sinkIsDone) {
                    simulator.addEvent(new StopEvent(simulator, STOP_TIME));
                } else {
                    sourceIsDone = true;
                }
            }

        } // endif sendingLastPacket

        if (node.nodeID == 0) {

            remainingBytesToSend -= APPLICATION_PACKET_SIZE;
            long nextSendTime;
            /*
             * TODO: this is of course cheating. but how to do it?? Fact: we
             * know that node 1 will need much more time to forward packet nr 1
             * also in the policy-case, since it needs to wake node 2 up.
             * sooo...?
             */
            if (sentPacketCount == 1) {
                /*
                 * this requires the initiator to know how many hops there are
                 * in the chain...! Or, well, after forwarding past the closest
                 * two hops, the nodes further away should be able to send
                 * (packet 0) concurrently with node 0 & 1 (sending/receving
                 * packet 1)
                 */
                nextSendTime = simulator.getSimulationTime() + (MessageToPacketLayer.MAX_PACKET_SEND_INTERVAL / 3)
                        * (numberOfNodes + 3); // packetSendInterval; //180000 ;
                                               // //packetSendInterval;
                                               // //PACKET_SEND_INTERVAL;
                if (0 < debug) {
                    System.out.println(node.nodeID + " is setting mega-delay. Is using " + nextSendTime);
                }

            } else {
                nextSendTime = simulator.getSimulationTime() + packetSendInterval;
            }
            if (APPLICATION_PACKET_SIZE < remainingBytesToSend) {
                if (1 < debug) {
                    System.out.println(node.nodeID + " is sending next packet " + (sentPacketCount + 1) + " with "
                            + remainingBytesToSend + " more bytes to go at time " + nextSendTime);
                }
                packetSendProtocol.send(new Packet(node.nodeID + 1, currentMessageId, sentPacketCount,
                        APPLICATION_PACKET_SIZE, node, MessageTypes.BULK_MESSAGE), nextSendTime);

            } else if (0 < remainingBytesToSend && remainingBytesToSend <= APPLICATION_PACKET_SIZE) {
                sendingLastPacket = true;
                if (1 < debug) {
                    System.out.println(node.nodeID + ": sending LAST packet " + (sentPacketCount + 1)
                            + " of current message: " + sentMessageCount + " at time " + nextSendTime);
                }
                packetSendProtocol.send(new Packet(node.nodeID + 1, currentMessageId, sentPacketCount,
                        remainingBytesToSend, node, MessageTypes.LAST_BULK_MESSAGE), nextSendTime);

            } // else we are already done!
        } // endif node 0
        else if (packetThatNeedsToBeAcced != null) {

            System.out.println(node.nodeID + ": sending ACC after forwarding for packet "
                    + packetThatNeedsToBeAcced.packetId + " at time " + simulator.getSimulationTime());
            packetSendProtocol.send(new Packet(packetThatNeedsToBeAcced.getSource().nodeID,
                    packetThatNeedsToBeAcced.messageId, packetThatNeedsToBeAcced.packetId,
                    RadioNodeInterface.MESSAGE_ACK_SIZE, node, MacLayer.PACKET_ACK));
            packetThatNeedsToBeAcced = null;
            waitingToCompletePacketAcc = currentMessageId;
        }

    }// end of transmissionComplete

    @Override
    public void transmissionFailed(long time) {
        System.err.println(node.nodeID + " FAILURE. Stopping run at time " + time);
        simulator.stop();

    }

    @Override
    public boolean finish() {
        if (0 < errorCount) {
            System.out.println(node.nodeID + ": Bulksend finished with " + errorCount
                    + " errors in transmission. A total of " + bytesLost + " bytes were lost, corresponding to "
                    + (bytesLost / APPLICATION_PACKET_SIZE) + " packets");
        }
        return errorCount == 0;
    }

    private void doneSendingMessage() {
        activeMessageSendTime += (simulator.getSimulationTime() - lastMessageSendStartTime);

    }

    private void startSendingMessage() {
        lastMessageSendStartTime = simulator.getSimulationTime();
    }

    private void startSendingMessage(long time) {
        lastMessageSendStartTime = time;
    }

    @Override
    public long getActiveMessageSendTime() {
        return activeMessageSendTime;
    }

    @Override
    public int getMessagesReceivedCount() {
        return receivedMessageCount;
    }

    @Override
    public int getMessagesSentCount() {
        return sentMessageCount;
    }

    @Override
    public long getPacketSendInterval() {

        return packetSendInterval;
    }

}
