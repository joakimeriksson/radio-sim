package se.sics.sim.core;

import com.botbox.scheduler.*;

/**
 * Describe class TimerEvent here.
 *
 *
 * Created: Tue Apr 24 17:21:52 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class TimerEvent extends TimeEvent {

    private TimerEventListener listener;
    private Object data;

    public TimerEvent(TimerEventListener listener, long time) {
        this(listener, time, null);
    }

    public TimerEvent(TimerEventListener listener, long time, Object data) {
        super(time);
        this.data = data;
        this.listener = listener;
    }

    public void execute(long time) {
        listener.timerEvent(time, data);
    }

}
