package se.sics.sim.interfaces;

import se.sics.sim.net.TransmissionEvent;

public interface RadioNodeInterface {

    public static final int PREAMBLE_SIZE = 4;
    public static final int PREAMBLE_ACK_SIZE = 4;
    public static final int MESSAGE_ACK_SIZE = PREAMBLE_ACK_SIZE;

    public static final int TRANSMIT_STARTUP_DELAY = 100;
    public static final int TRANSMIT_DELAY = 20;

    public static final int RADIO_SPEED = 250000; // 56600; //250000; //BPS
    public static final int MICROSEC_PER_BYTE_SENDING = 1000000 / (RADIO_SPEED / 8);
    public static final int RADIO_INIT_TRANSMIT_TIME = 100; // 0.1ms

    public static final int COPY_RATE = 250000 / 192; // 128; //64; //8; //as
                                                      // fast as the radio,
                                                      // perhaps...
    public static final int MICROSEC_PER_BYTE_COPYING = 1000000 / COPY_RATE;

    public void on(long time);

    public void off(long time);

    public void send(TransmissionEvent transmissionEvent);

    public void addPacketReceiver(MacLayer packetReceiver);

    public void transmissionComplete(long time, int type);

    public boolean isReceiving();

    // just for statistics:
    public void startSendingPacket(long time);

    public void doneSendingPacket();

}
