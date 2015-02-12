package se.sics.sim.net;

import se.sics.sim.core.Message;
import se.sics.sim.core.Node;
import se.sics.sim.interfaces.MacLayer;
import se.sics.sim.interfaces.MessageToPacketLayer;
import se.sics.sim.interfaces.RadioModes;
import se.sics.sim.interfaces.RadioNodeInterface;

public class CC2420Node extends Node implements RadioNodeInterface {

    private boolean stopOnCollision = true;

    int radioMode = 0; // currently: on = 1, off = 0
    private int receive = 0;
    private MacLayer packetReceiver;
    private long lastRadioOffTime = 0;
    int radioOffCount = 0;
    long lastPacketSendStartTime = 0;
    long activePacketSendTime = 0;

    long lastTransmitStartTime = 0;
    long activeTransmitTime = 0;
    // int packetsReceived = 0;
    int packetsIgnored = 0;
    boolean reportStatus = true;

    private int debug = 0; // 1;

    private long currentUtility = 0;;

    // private int currentUtility = 0;;

    @Override
    public void on(long time) {
        if (time != 0 && time == lastRadioOffTime) {
            // try {
            // int error = 1 / 0;
            // }catch(Exception e) {
            // System.out.println(nodeID + " LastRadioOffTim = onTime: " +
            // time);
            // e.printStackTrace();
            // }
        }
        if (radioMode == 0) {
            // update statistics
            radioOffTime += (time - lastRadioOffTime);
        }
        radioMode = RadioModes.MODE_LISTEN;
    }

    @Override
    public void off(long time) {
        if (radioMode == 1) {
            // update statistics
            radioOffCount++;
            lastRadioOffTime = time;
        }
        radioMode = RadioModes.MODE_OFF;
    }

    @Override
    public void addPacketReceiver(MacLayer packetReceiver) {
        this.packetReceiver = packetReceiver;
    }

    @Override
    public void send(TransmissionEvent transmissionEvent) {
        // do we want change something / add some more delays...?
        lastTransmitStartTime = transmissionEvent.getTime();
        simulator.addEvent(transmissionEvent);

    }

    // @Override
    /*
     * public void packetReceived(long time, Packet packet) { // TODO
     * Auto-generated method stub
     * 
     * }
     */

    @Override
    public boolean isReceiving() {
        return receive != 0;
    }

    @Override
    public void messageReceived(long time, Message message) {

        if (message instanceof TransmissionEvent) {
            TransmissionEvent te = (TransmissionEvent) message;
            if (radioMode == RadioModes.MODE_LISTEN) {
                if (1 < debug) {
                    System.out.println(nodeID + " incoming msg at time " + time + " msg-type: " + te.type
                            + " transmission-type: " + te.transmissionType + " no of receivers: " + receive);
                }
                if (te.transmissionType == TransmissionEvent.START) {
                    receive++;
                } else {
                    if (receive > 0) {
                        receive--;
                        if (receive > 0) {
                            /* oops multiple nodes was sending at the same time */
                            if (0 < debug) {
                                System.out.println(nodeID + " msg trashed, collision at time " + time
                                        + " of transmission type " + te.transmissionType);
                            }
                            if (stopOnCollision) {
                                reportStatus = false;
                                simulator.stop();
                            }
                        } else {
                            packetReceiver.receivePacket(te);
                        }
                    } else {
                        if (1 < debug) {
                            System.out.println(nodeID + " missed the transmission start");
                        }
                    }
                }
            } else {
                packetsIgnored++;
                if (1 < debug) {
                    System.out.println(nodeID + " incoming msg IGNORED");
                }
                /* not listening any more - reset receive */
                receive = 0;
            }
        }
    }

    @Override
    public void transmissionComplete(long time, int type) {
        // System.err.println("Accing with type " + type);
        activeTransmitTime += (time - lastTransmitStartTime);
        macLayer.transmissionComplete(time, type);

    }

    @Override
    public void doneSendingPacket() {
        activePacketSendTime += (simulator.getSimulationTime() - lastPacketSendStartTime);

    }

    @Override
    public void startSendingPacket(long time) {
        lastPacketSendStartTime = time; // simulator.getSimulationTime();
    }

    @Override
    public void init() {
        radioMode = 0; // currently: on = 1, off = 0
        receive = 0;
        lastRadioOffTime = 0;
        radioOffCount = 0;
        lastPacketSendStartTime = 0;
        activePacketSendTime = 0;
        currentUtility = Long.parseLong((String) simulator.getProperty("simulation.maxUtility"));
        // System.out.println("cc-node says max is " + currentUtility);
    }

    @Override
    public boolean finish() {
        return finish(debug == 0);
    }

    public boolean finish(boolean silent) {

        reportStatus = reportStatus && applicationLayer.finish() && macLayer.finish();

        if (!reportStatus) {
            if (0 < debug) {
                System.out.println("Node " + nodeID + " fishished with errors!");
            }
            preambleAccCount = 0;
            preambleCount = 0;
            return false;
        } else if (silent) {
            return true;
        }

        System.out.println("*************** Node " + nodeID + " ******************************");
        System.out.println("In total " + bytesSent + " bytes sent.");

        // " activeTransmitTime " + activeTransmitTime + " activeSendTime: " +
        // activeSendTime + "\tignored/rec: " + (packetsIgnored) + " "
        // +(packetsReceived));

        // assuming the bytes between message-lenght & N*PacketSize and
        // neglectable
        long usefulDataSent = MessageToPacketLayer.APPLICATION_PACKET_SIZE * packetsSentCount;
        long usefulDataReceived = MessageToPacketLayer.APPLICATION_PACKET_SIZE * packetsReceivedCount;

        // Sending messages
        System.out.println(packetsSentCount + " packets sent, corresponding to " + usefulDataSent + " bytes.");
        System.out.println(preambleCount + " preables sent, corresponding to " + RadioNodeInterface.PREAMBLE_SIZE
                * preambleCount + " bytes.");

        // Receivng messages
        System.out.println(packetsReceivedCount + " packets received, corresponding to " + usefulDataReceived
                + " bytes.");
        System.out.println(preambleAccCount + " preable-accs sent, corresponding to "
                + RadioNodeInterface.PREAMBLE_ACK_SIZE * preambleAccCount + " bytes.");
        System.out.println(messageAccCount + " message-accs sent, corresponding to "
                + RadioNodeInterface.MESSAGE_ACK_SIZE * messageAccCount + " bytes.");

        // sanity-check:
        // 1 preable + 1 inbetween-delay =
        double strobesPerPacket = (((double) preambleCount) / ((double) packetsSentCount));
        double perPreambleDelay = RadioNodeInterface.MICROSEC_PER_BYTE_SENDING
                + (RadioNodeInterface.TRANSMIT_DELAY + RadioNodeInterface.PREAMBLE_SIZE
                        * RadioNodeInterface.MICROSEC_PER_BYTE_SENDING);
        double estimatedPerPackageDelay = strobesPerPacket * perPreambleDelay + (strobesPerPacket - 1)
                * macLayer.getStrobeInterval();
        System.out.println("Ideal time per packet: 1 preable + 1 preable acc + 1 packet + 1 packet acc: "
                + MessageToPacketLayer.IDEAL_TIME_UNTIL_PACKET_ACC);
        System.out.println("Used between packet-delay: " + messageToPacketLayer.getPacketSendInterval()
                + " vs ideal between packet-delay: " + MessageToPacketLayer.IDEAL_PACKET_SEND_INTERVAL);
        System.out.println("strobesPerPacket " + strobesPerPacket + " <=> estimated delay per packet: "
                + estimatedPerPackageDelay);
        double timePerPacket = 0 < packetsSentCount ? (activePacketSendTime / packetsSentCount) : -1;
        System.out.println("ActivePacketSendTime was " + activePacketSendTime + " <=> time per sent packet "
                + timePerPacket + " <=> useful bytes per second: "
                + (usefulDataSent / (activePacketSendTime / 1000000.)));

        long activeMessageSendTime = messageToPacketLayer.getActiveMessageSendTime();
        int messagesSentCount = messageToPacketLayer.getMessagesSentCount();

        double timePerMessage = 0 < messagesSentCount ? (activeMessageSendTime / messagesSentCount) : -1;
        System.out.println("ActiveMessageSendTime was " + activeMessageSendTime + " <=> time per sent message "
                + timePerMessage + " <=> useful bytes per second: "
                + (usefulDataSent / (activeMessageSendTime / 1000000.)));

        double radioOffFraction = ((double) radioOffTime) / ((double) simulator.getSimulationTime());
        double energyScalingFactor = 0.0001;
        double energyUsedPerByte = energyScalingFactor * ((1 - radioOffFraction) * simulator.getSimulationTime())
                / usefulDataSent;
        System.out.println("Radio was turned off " + radioOffCount + " times = " + radioOffTime
                + " us, corresponding to " + radioOffFraction + " <=> 'energy used per useful byte' = "
                + energyUsedPerByte);

        return true;
    }

    public void addUtilityPenalty(int penalty) {
        currentUtility -= penalty;
        // System.out.println("Utility after reduction: " + currentUtility);
    }

    public long getUtility() {
        // Bigger is better!
        long utility;
        if (!reportStatus) {
            utility = -Integer.MAX_VALUE;
            System.out.println("Baad");
        }
        // else if(currentUtility < 0) {
        // of course, this should also dep on the utility function, but for now
        // they use the same
        // utility = currentUtility;
        // }
        else {
            // System.out.println("currentUtility " + currentUtility +
            // " getActiveMessageSendTime() " +
            // messageToPacketLayer.getActiveMessageSendTime() + " radioOnTime "
            // + (simulator.getSimulationTime() - radioOffTime) +
            // " (radioOffTime " +radioOffTime + ")");
            switch (UTILITY_FUNCTION) {
            case ENERGY:
                // utility = currentUtility + radioOffTime;
                utility = currentUtility - (simulator.getSimulationTime() - radioOffTime);
                break;
            case TIME:
                utility = currentUtility - messageToPacketLayer.getActiveMessageSendTime();
                break;
            case TIME_AND_ENERGY:
                // utility = currentUtility -
                // messageToPacketLayer.getActiveMessageSendTime() +
                // radioOffTime; //was: simulator.getS
                // TIME_AND_SOME_ENERGY
                // utility = currentUtility -
                // messageToPacketLayer.getActiveMessageSendTime() +
                // (radioOffTime / 10); //was: simulator.getS

                // utility = currentUtility -
                // messageToPacketLayer.getActiveMessageSendTime() +
                // (radioOffTime); // / 5);
                // utility = - messageToPacketLayer.getActiveMessageSendTime() +
                // (radioOffTime); // / 5);
                utility = currentUtility - (simulator.getSimulationTime() - radioOffTime)
                        - messageToPacketLayer.getActiveMessageSendTime();
                break;
            default:
                utility = currentUtility - (simulator.getSimulationTime() - radioOffTime)
                        - messageToPacketLayer.getActiveMessageSendTime();

            }
            // System.out.println("Utility: " + utility);

        }
        if (1 < debug) {
            System.out.println(nodeID + " u: " + utility + " ast: " + messageToPacketLayer.getActiveMessageSendTime());
        }
        return utility;
        // return currentUtility - (simulator.getSimulationTime()); // -
        // radioOffTime);
    }

}
