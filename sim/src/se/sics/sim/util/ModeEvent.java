package se.sics.sim.util;

import com.botbox.scheduler.TimeEvent;

public class ModeEvent extends TimeEvent {

    public static final int OFF = 0;
    public static final int ON = 1;

    private ModeListener listener;
    private int newMode;

    public ModeEvent(long time, int newMode, ModeListener listener) {
        super(time);
        this.newMode = newMode;
        this.listener = listener;
    }

    public void execute(long time) {
        listener.modeChanged(newMode);
    }

}
