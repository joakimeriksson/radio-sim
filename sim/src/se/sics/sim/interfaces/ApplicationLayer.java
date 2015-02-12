package se.sics.sim.interfaces;

import se.sics.sim.core.Node;

public interface ApplicationLayer extends PacketReceiver {

    public void init(Node node);

    public boolean finish();
}
