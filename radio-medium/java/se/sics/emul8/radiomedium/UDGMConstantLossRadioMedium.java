package se.sics.emul8.radiomedium;

/**
 * The Unit Disk Graph Radio Medium abstracts radio transmission range as circles.
 */
public class UDGMConstantLossRadioMedium extends AbstractRadioMedium {

    double range = 100.0; /* hundred meters range of the communication */

    @Override
    public String getName() {
        return "UDGM Constant Loss Radio Medium";
    }

    @Override
    public void transmit(RadioPacket packet) {
        Node[] nodes = simulator.getNodes();
        if (nodes == null) {
            return;
        }

        double rssi = packet.getTransmitPower();
        int channel = packet.getWirelessChannel();
        Node source = packet.getSource();
        for (Node node : nodes) {
            if (node != source) {
                Transciever radio = node.getRadio();
                if (radio.isEnabled() && radio.getWirelessChannel() == channel) {
                    double distance = source.getPosition().getDistance(node.getPosition());
                    if(distance < range) {
                        simulator.deliverRadioPacket(packet, node, rssi);
                    }
                }
            }
        }
    }
}
