package se.sics.emul8.radiomedium;
import java.util.Random;

/**
 * The Matrix Radio Medium abstracts radio transmission range as probabilities specified in a matrix.
 */
public class N2NRadioMedium extends AbstractRadioMedium {

    private double[][] matrix;

    public N2NRadioMedium(double[][] m) {
        this.matrix = m;
    }

    @Override
    public String getName() {
        return "Matrix Radio Medium";
    }

    public void setMatrix(double[][] matrix) {
        this.matrix = matrix;
    }

    public double getTxSuccessProbability(Node source) {
        return source.getRadio().getTxProbability();
    }

    public double getRxSuccessProbability(Node source, Node dest) {
        int sourceID = source.getIdAsInteger();
        int destID = dest.getIdAsInteger();
        if (matrix == null || sourceID <= 0 || destID <= 0
                || sourceID > matrix.length || matrix[sourceID - 1] == null
                || destID > matrix[sourceID - 1].length) {
            return 0.0;
        }
        return matrix[sourceID - 1][destID - 1] * dest.getRadio().getRxProbability();
    }

    @Override
    public void transmit(RadioPacket packet) {
        Random random = simulator.getRandom();
        Node source = packet.getSource();
        double txSuccess = getTxSuccessProbability(source);
        boolean interference = false;
        if (txSuccess <= 0.0 || (txSuccess < 1.0 && random.nextDouble() > txSuccess)) {
            /* Node failed to send */
            interference = true;
        }

        Node[] nodes = simulator.getNodes();
        double rssi = packet.getTransmitPower();
        int channel = packet.getWirelessChannel();
        simulator.generateTransmissionEvents(packet);
        if (nodes != null) {
            for (Node node : nodes) {
                if (node != source) {
                    Transciever radio = node.getRadio();
                    if (radio.isEnabled() && radio.getWirelessChannel() == channel) {
                        double rxSuccess = getRxSuccessProbability(source, node);
                        if (rxSuccess <= 0.0) {
                            // The receiver can not hear the sender
                        } else if (interference || (rxSuccess < 1.0 && random.nextDouble() > rxSuccess)) {
                            /* Destination failed to receive but is interfered */
                            simulator.generateReceptionEvents(packet, node, rssi);
                        } else {
                            simulator.deliverRadioPacket(packet, node, rssi);
                            simulator.generateReceptionEvents(packet, node, rssi);
                        }
                    }
                }
            }
        }
    }
}
