package se.sics.emul8.radiomedium;

/**
 * The Unit Disk Graph Radio Medium abstracts radio transmission range as circles.
 *
 * It uses two different range parameters: one for transmissions, and one for
 * interfering with other radios and transmissions.
 *
 * For radio transmissions within range, two different success ratios are used [0.0-1.0]:
 * one for successful transmissions, and one for successful receptions.
 * If the transmission fails, no radio will hear the transmission.
 * If one of receptions fail, only that receiving radio will not receive the transmission,
 * but will be interfered throughout the entire radio connection.
 *
 * The received radio packet signal strength grows inversely with the distance to the
 * transmitter.
 */
public class UDGMRadioMedium extends AbstractRadioMedium {

    double range = 100.0; /* hundred meters range of the communication */

    @Override
    public void transmit(RadioPacket packet) {
        Node[] nodes = simulator.getNodes();
        double rssi = 0.0;
        Node source = packet.getSource();
        if (nodes != null) {
            for (Node node : nodes) {
                if (node != source) {
                    double distance = source.getPosition().getDistance(node.getPosition());
                    if(distance < range) {
                      simulator.deliverRadioPacket(packet, node, rssi);
                    }
                }
            }
        }
    }

}
