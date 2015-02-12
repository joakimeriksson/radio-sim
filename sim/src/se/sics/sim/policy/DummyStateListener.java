package se.sics.sim.policy;

import se.sics.sim.core.Node;
import se.sics.sim.core.Simulator;
import se.sics.sim.core.Node.State;
import se.sics.sim.interfaces.StateListener;

public class DummyStateListener implements StateListener {

    @Override
    public void stateChanged(Node source, State state, int value) {
        // Do nuffin
    }

    @Override
    public void finish(Simulator simulator) {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(Simulator sim) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getPolicy() {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public Object getPolicyAsText() {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public void stateChanged(Node source, State state, int value, long time) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reinit(Simulator sim) {
        // TODO Auto-generated method stub

    }

}
