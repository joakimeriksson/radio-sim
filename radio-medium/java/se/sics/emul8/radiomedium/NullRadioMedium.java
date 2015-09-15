package se.sics.emul8.radiomedium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullRadioMedium extends AbstractRadioMedium {

    private static final Logger log = LoggerFactory.getLogger(NullRadioMedium.class);

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "Null radio medium - just forwards incoming packets to all other nodes";
    }

    @Override
    public void transmit(RadioPacket packet) {
        Simulator simulator = this.simulator;
        if (simulator == null) {
            log.error("No simulator");
            return;
        }

        // Send packet to all nodes
        Node[] nodes = simulator.getNodes();
        double rssi = 0.0;
        if (nodes != null) {
            for (Node node : nodes) {
                if (node != packet.getSource()) {
                    simulator.deliverRadioPacket(packet, node, rssi);
                }
            }
        }
    }

}
