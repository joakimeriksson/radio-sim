package se.sics.sim.interfaces;

import se.sics.sim.core.Node;
import se.sics.sim.core.Simulator;

public interface StateListener {
    public void stateChanged(Node source, Node.State state, int value);

    public void stateChanged(Node source, Node.State state, int value, long time);

    public void finish(Simulator simulator);

    public void init(Simulator sim);

    public Object getPolicyAsText();

    public String getPolicy();

    void reinit(Simulator sim);
}
