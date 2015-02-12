package se.sics.sim.interfaces;

import se.sics.sim.core.Message;
import se.sics.sim.core.Node;

public interface PacketReceiver {
    // public void receivePacket(Packet packet);
    public void receivePacket(Message message);

    public void setProperty(Node.State state, int value);
}
