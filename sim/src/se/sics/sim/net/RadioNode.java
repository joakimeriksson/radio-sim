package se.sics.sim.net;

import se.sics.sim.core.Message;
import se.sics.sim.core.Node;
import se.sics.sim.core.Simulator;
import se.sics.sim.examples.LineSetup;
import se.sics.sim.util.ModeEvent;
import se.sics.sim.util.ModeListener;
import com.botbox.scheduler.TimeEvent;

public class RadioNode extends Node implements ModeListener {

    /* this radio node currently has a built-in XMAC:ish protocol */
    public static final long LISTEN_TIME = 10000;
    public static final long SLEEP_TIME = 90000;
    public static final long PREAMBLE_TIME = 8000;
    public static final long SEND_INTERVAL = 300000;
    public static final long RADIO_INIT_TRANSMIT_TIME = 100; // 0.1ms
    public static final int PREAMBLE_COUNT = 14;

    public static final int PREAMBLE = 1;
    public static final int PREAMBLE_ACK = 2;
    public static final int MESSAGE = 3;
    public static final int MESSAGE_ACK = 4;

    public static final int MODE_OFF = 0;
    public static final int MODE_LISTEN = 1;
    public static final int MODE_TRANSMIT = 2;
    public static final int MODE_MAX = MODE_TRANSMIT;
    public static final String MODE_NAME[] = new String[] { "Off", "Listen", "Transmit" };

    public long accTime[] = new long[MODE_MAX + 1];

    private final int NUMBER_OF_TEST_MESSAGES = 55;

    private int radioMode;
    private int receive = 0;
    private boolean transmit = false;
    private boolean xmacSending = false;

    private long lastTime;

    private static boolean sinkIsDone = false;
    private static boolean sourceIsDone = false;

    TimeEvent xmacOnOff = new TimeEvent(0) {
        public void execute(long currentTime) {
            int nm = 0;
            if (radioMode != MODE_LISTEN) {
                nm = MODE_LISTEN;
                time += LISTEN_TIME;
                // if (nodeID == 0) System.out.println("XMAC-listen");
                simulator.addEvent(this);
            } else {
                nm = 0;
                time += SLEEP_TIME;
                // if (nodeID == 0) System.out.println("XMAC-sleep");
                // System.err.println(nodeID + " Lemme guess - is 'ere!?");
                simulator.addEvent(this);
            }
            if (!transmit && !xmacSending && receive == 0) {
                /* only if we are in non transmit mode ... */
                changeMode(nm, currentTime);
            } else {
                System.out.println(nodeID + " Changing mode ignored...");
            }
        }
    };

    public class XMACSender extends TimeEvent {
        int preamblesLeft = PREAMBLE_COUNT;
        int dstID = 0;
        int id = 0; // message packetId
        int size = 0;

        public XMACSender() {
            super(0);
        }

        public void sendMessage(Simulator sim, int dstID, int id, int size) {
            if (this.size == 0) {
                this.id = id;
                this.size = size;
                this.dstID = dstID;
                preamblesLeft = PREAMBLE_COUNT;
                time = sim.getSimulationTime() + RADIO_INIT_TRANSMIT_TIME;
                sim.addEvent(this);
                xmacSending = true;
                changeMode(MODE_LISTEN, sim.getSimulationTime());
            }
        }

        public void execute(long ctime) {
            if (xmacSending) {
                System.out.println(nodeID + " sending preamble  left: " + preamblesLeft + " " + ctime);
                transmitMessage(PREAMBLE, dstID, id, 4);
                preamblesLeft--;
                if (preamblesLeft > 0) {
                    time = ctime + PREAMBLE_TIME;
                    simulator.addEvent(this);
                } else {
                    xmacSending = false;
                }
            }
        }

        public void sendPreambleAck(int dstID, int id) {
            transmitMessage(PREAMBLE_ACK, dstID, id, 4);
        }

        public void preambleAckReceived(int id) {
            if (this.id == id) {
                System.out.println(nodeID + " Transmitting message!!!");
                transmitMessage(MESSAGE, dstID, id, size);
                size = 0;
                xmacSending = false;
            }
        }
    }

    XMACSender xmacSender = new XMACSender();

    private void changeMode(int newMode, long time) {
        // System.out.println(nodeID + " Changing mode to: " +
        // MODE_NAME[newMode]
        // + " time: " + time);
        accTime[radioMode] += time - lastTime;
        lastTime = time;
        radioMode = newMode;
    }

    @Override
    public void init() {
        int start = (int) (Math.random() * 100000);
        xmacOnOff.setTime(start);
        simulator.addEvent(xmacOnOff);
        if (nodeID == 0) {
            TimeEvent sender = new TimeEvent(0) {
                int id = 0;

                public void execute(long currentTime) {
                    if (id < NUMBER_OF_TEST_MESSAGES) {
                        System.out.println(nodeID + " Sending message with packetId " + (id + 1));
                        this.time = currentTime + SEND_INTERVAL;
                        xmacSender.sendMessage(simulator, nodeID + 1, id++, 42);
                        simulator.addEvent(this);
                    } else {
                        System.out.println("#### All packets sent! ####");
                        if (sinkIsDone) {
                            simulator.stop();
                        } else {
                            sourceIsDone = true;
                        }
                    }
                }
            };
            /* send one packet immediately */
            sender.execute(0);
        }
    }

    public void modeChanged(int newMode) {
        radioMode = newMode;
        if (radioMode == ModeEvent.ON) {
            receive = 0;
        }
    }

    // public void packetReceived(long time, Packet packet) {
    // if(packet instanceof Message) {
    // messageReceived(time, (Message)packet);
    // } else {
    // System.err.println("Historical error");
    // }
    // }

    public void messageReceived(long time, Message message) {
        if (message instanceof TransmissionEvent) {
            TransmissionEvent te = (TransmissionEvent) message;
            if (radioMode == MODE_LISTEN) {
                System.out.println(nodeID + " incoming msg (listen)  type: " + te.type + " tType: "
                        + te.transmissionType + " receive: " + receive);
                if (te.transmissionType == TransmissionEvent.START) {
                    receive++;
                } else {
                    if (receive > 0) {
                        receive--;
                        if (receive > 0) {
                            /* oops multiple nodes was sending at the same time */
                            System.out.println(nodeID + " msg trashed (collission)");
                        } else {
                            handleMessage(te);
                        }
                    } else {
                        System.out.println(nodeID + " missed transmission start?");
                    }
                }
            } else {
                System.out.println(nodeID + " incoming msg ignored (not listen)");
                /* not listening any more - reset receive */
                receive = 0;
            }
        }
    }

    int preMode = 0;

    public void transmitMessage(int msgType, int dstID, int id, int size) {
        /* here we need to schedule a transmission event */
        long time = simulator.getSimulationTime();
        transmit = true;
        preMode = radioMode;
        changeMode(MODE_TRANSMIT, time);
        time += 100; /* add some time for starting up transmission ??? */
        simulator.addEvent(new TransmissionEvent(3, msgType, 0, id, size, this, getLinks(), dstID, time, simulator));
    }

    public void transmissionComplete(long time) {
        transmit = false;
        changeMode(preMode, time);
    }

    private void handleMessage(TransmissionEvent te) {
        if (te.dstID != nodeID) {
            System.out.println(nodeID + "  message ignored: for: " + te.dstID);
            return;
        }
        switch (te.type) {
        case PREAMBLE:
            System.out.println(nodeID + "  *** PREAMBLE received " + te.getTime());
            /* check if it is to me ??? */
            xmacSender.sendPreambleAck(te.getSource().nodeID, te.packetId);
            break;
        case PREAMBLE_ACK:
            System.out.println(nodeID + "  *** PREAMLBE ACK received " + te.getTime());
            xmacSender.preambleAckReceived(te.getID());
            break;
        case MESSAGE:
            System.out.println(nodeID + "  *** MESSAGE received from: " + te.getSource().nodeID + " type: " + te.type
                    + " with packetId " + te.packetId + " time " + te.getTime());
            if (nodeID < LineSetup.NUMBER_OF_NODES - 1) { // since node packetId
                                                          // starts from 0,
                                                          // lastNodeId is
                                                          // LINE_LENGTH-1
                xmacSender.sendMessage(simulator, nodeID + 1, te.packetId++, 42);
            } else {
                System.out.println("Node " + nodeID + " has received message no " + te.packetId);
                if (te.packetId + 1 == NUMBER_OF_TEST_MESSAGES) { // last node
                                                                  // in chain
                                                                  // decides
                                                                  // when the
                                                                  // fat lady
                                                                  // has sung
                    if (sourceIsDone) {
                        simulator.stop();
                    } else {
                        sinkIsDone = true;
                    }
                }
            }

            break;
        }
    }
}
