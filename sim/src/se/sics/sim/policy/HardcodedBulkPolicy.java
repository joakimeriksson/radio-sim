package se.sics.sim.policy;

import se.sics.sim.core.Node;
import se.sics.sim.core.Simulator;
import se.sics.sim.core.Node.State;
import se.sics.sim.interfaces.StateListener;

public class HardcodedBulkPolicy implements StateListener {

    // TODO
    int off;

    public HardcodedBulkPolicy() {
        System.out.println("HardcodedBulkPolicy is instanciated at startup");
    }

    @Override
    public void stateChanged(Node source, State state, int value) {
        if (state == Node.State.BULK) {
            if (value == 1) {
                // Force off-time to 0 when starting bulk-mode
                source.setProperty(Node.State.MAC_OFF_TIME, 0);
            } else {
                // Reset off-time to normal when finishing bulk-mode
                source.setProperty(Node.State.MAC_OFF_TIME, 1);

            }
        }
        // else - don't go here, or we will loop...

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
    public Object getPolicyAsText() {
        return "if (state == Node.State.BULK) {\n" + "\tif (value == 1) {"
                + "\t\tsource.setProperty(Node.State.MAC_OFF_TIME, 0);" + "\t} else {"
                + "\t\tsource.setProperty(Node.State.MAC_OFF_TIME, 1);" + "\t}" + "}";

    }

    @Override
    public String getPolicy() {
        return "";
    }

    @Override
    public void stateChanged(Node source, State state, int value, long time) {
        // ignore time
        stateChanged(source, state, value);

    }

    @Override
    public void reinit(Simulator sim) {
        // TODO Auto-generated method stub

    }

}
