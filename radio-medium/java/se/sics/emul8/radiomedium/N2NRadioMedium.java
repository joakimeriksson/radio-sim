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
        if (matrix == null || sourceID < 0 || destID < 0
                || sourceID >= matrix.length || matrix[sourceID] == null
                || destID >= matrix[sourceID].length) {
            return 0.0;
        }
        return matrix[sourceID][destID] * dest.getRadio().getRxProbability();
    }

    @Override
    public void transmit(RadioPacket packet) {
        Random random = simulator.getRandom();
        Node source = packet.getSource();
        double txSuccess = getTxSuccessProbability(source);
        if (txSuccess <= 0.0 || (txSuccess < 1.0 && random.nextDouble() > txSuccess)) {
            /* Node failed to send */
            return;
        }

        Node[] nodes = simulator.getNodes();
        double rssi = packet.getTransmitPower();
        int channel = packet.getWirelessChannel();
        if (nodes != null) {
            for (Node node : nodes) {
                if (node != source) {
                    Transciever radio = node.getRadio();
                    if (radio.isEnabled() && radio.getWirelessChannel() == channel) {
                        double rxSuccess = getRxSuccessProbability(source, node);
                        if (rxSuccess <= 0.0 || (rxSuccess < 1.0 && random.nextDouble() > rxSuccess)) {
                            /* Destination failed to receive */
                            continue;
                        }
                        simulator.deliverRadioPacket(packet, node, rssi);
                    }
                }
            }
        }
    }
}
