package se.sics.emul8.radiomedium.events;

import com.botbox.scheduler.TimeEvent;

import se.sics.emul8.radiomedium.Node;
import se.sics.emul8.radiomedium.RadioPacket;
import se.sics.emul8.radiomedium.Simulator;
import se.sics.emul8.radiomedium.Transciever;

public class ReceptionEvent extends TimeEvent {

    public static enum ReceptionMode {
        start,
        interference,
        delivery
    };

    private RadioPacket packet;
    private Node destination;
    private double rssi;
    private ReceptionMode mode;
    private Simulator simulator;

    public ReceptionEvent(long time, Simulator simulator, RadioPacket packet, Node destination,
            double rssi, ReceptionMode mode) {
        super(time);
        this.mode = mode;
        this.packet = packet;
        this.destination = destination;
        this.rssi = rssi;
        this.simulator = simulator;
    }

    @Override
    public void execute(long currentTime) {
        Transciever t = this.destination.getRadio();
        if (mode == ReceptionMode.start) {
            t.setReceiving(packet, rssi);
        } else {
            t.clearReceiving();
            if (mode == ReceptionMode.delivery) {
                /* Only deliver on the end flank */
                simulator.deliverRadioPacket(packet, destination, rssi);
            }
        }
    }

    @Override
    public String toString() {
        return "ReceptionEvent[" + this.time + "," + this.mode
                + "," + destination.getId() + "," + packet + "]";
    }

}
