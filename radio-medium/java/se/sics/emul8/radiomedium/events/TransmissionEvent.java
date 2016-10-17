package se.sics.emul8.radiomedium.events;

import com.botbox.scheduler.TimeEvent;

import se.sics.emul8.radiomedium.Node;
import se.sics.emul8.radiomedium.RadioPacket;
import se.sics.emul8.radiomedium.Simulator;
import se.sics.emul8.radiomedium.Transciever;

public class TransmissionEvent extends TimeEvent {

    private RadioPacket packet;
    private Node destination;
    private double rssi;
    private boolean isStart = true;
    private Simulator simulator;

    public TransmissionEvent(long time, Simulator simulator, RadioPacket packet, Node destination,
            double rssi, boolean b) {
        super(time);
        this.isStart = b;
        this.packet = packet;
        if (b) {
            packet.setEndTime();
        }
        this.destination = destination;
        this.rssi = rssi;
        this.simulator = simulator;
    }

    @Override
    public void execute(long currentTime) {
        Transciever t = this.destination.getRadio();
        if(!isStart) {
            /* Only deliver on the end flank */
            System.out.println("Transmission event - delivering second transmission." + currentTime);
            t.clearReceiving();
            simulator.deliverRadioPacket(packet, destination, rssi);
        } else {
            System.out.println("Transmission event - not delivering first packet transmission - only second." + currentTime);
            t.setReceiving(packet, rssi);
        }
    }    
}
