package se.sics.sim.rl;

import se.sics.sim.core.Node.State;
import se.sics.sim.net.CC2420Node;
import com.botbox.scheduler.TimeEvent;

public class MakePolicyChangeEvent extends TimeEvent {

    CC2420Node source;
    public State state;
    public int value;

    public MakePolicyChangeEvent(CC2420Node source, State state, int value, long time) {
        super(time);
        this.source = source;
        this.state = state;
        this.value = value;
    }

    @Override
    public void execute(long currentTime) {
        // System.out.println(source.nodeID + " Executing policychange at time "
        // + time);
        source.setProperty(state, value);
        // System.out.println(source.nodeID + " value of property "+ state +
        // " is now " + value);

    }

}
