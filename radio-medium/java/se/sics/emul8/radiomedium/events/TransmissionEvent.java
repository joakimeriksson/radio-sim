package se.sics.emul8.radiomedium.events;
import com.botbox.scheduler.TimeEvent;
import se.sics.emul8.radiomedium.RadioPacket;
import se.sics.emul8.radiomedium.Simulator;
import se.sics.emul8.radiomedium.Transciever;

public class TransmissionEvent extends TimeEvent {

    private RadioPacket packet;
    private boolean isStart = true;

    public TransmissionEvent(long time, Simulator simulator, RadioPacket packet, boolean isStart) {
        super(time);
        this.isStart = isStart;
        this.packet = packet;
    }

    @Override
    public void execute(long currentTime) {
        Transciever t = this.packet.getSource().getRadio();
        if(isStart) {
            t.setSending(this.packet);
        } else {
            t.clearSending();
        }
    }
}
