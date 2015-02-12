package se.sics.sim.interfaces;

import se.sics.sim.core.Node;
import se.sics.sim.core.Packet;

public interface MacLayer extends PacketReceiver {

    public static final int PREAMBLE = 1;
    public static final int PREAMBLE_ACK = 2;
    public static final int PACKET = 3;
    public static final int PACKET_ACK = 4;

    public static int LINK_ACC_OFF = 0, LINK_ACC_BEFORE_FORWARD = 1, LINK_ACC_AFTER_FORWARD = 2;

    public static final long LISTEN_TIME_DEFAULT = 20 * 1000;
    public static final long SLEEP_TIME_DEFAULT = 480 * 1000;
    public static final long PREAMBLE_TIME = (long) (0.8 * LISTEN_TIME_DEFAULT); // should

    public void on();

    public void off();

    public void send(Packet packet);

    public void send(Packet packet, long time);

    public void setAccMode(int accMode);

    // if "ACC_AFTER_FORWARD" is used
    // public void sendAck(TransmissionEvent transmissionEvent);

    public void transmissionComplete(long time, int type);

    public void addPacketReceiver(PacketReceiver packetReceiver);

    public long getStrobeInterval();

    public void init(Node node);

    public boolean finish();
}
