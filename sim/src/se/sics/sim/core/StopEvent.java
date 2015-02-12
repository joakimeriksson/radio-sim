package se.sics.sim.core;

import com.botbox.scheduler.TimeEvent;

public class StopEvent extends TimeEvent {

    Simulator simulator;

    public StopEvent(Simulator simulator, long time) {
        super(time);
        this.simulator = simulator;

    }

    @Override
    public void execute(long currentTime) {
        // System.out.println("The stopevent is executed at time " +
        // currentTime);
        simulator.stop();

    }

}
