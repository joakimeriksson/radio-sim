package se.sics.emul8.radiomedium;
import java.util.Random;

/**
 * The Unit Disk Graph Radio Medium abstracts radio transmission range as circles.
 *
 * It uses two different range parameters: one for transmissions, and one for
 * interfering with other radios and transmissions.
 *
 * For radio transmissions within range, two different success ratios are used [0.0-1.0]:
 * one for successful transmissions, and one for successful receptions.
 * If the transmission fails, no radio will hear the transmission.
 * If one of receptions fail, only that receiving radio will not receive the transmission.
 */
public class UDGMRadioMedium extends AbstractRadioMedium {

    /* Success ratio of TX. If this fails, no radios receive the packet */
    private double successRatioTx    = 1.0;
    /* Success ratio of RX. If this fails, the specific receiver does not receive the packet */
    private double successRatioRx    = 1.0;
    /* Transmission range */
    private double transmissionRange = 50.0;
    /* Interference range.*/
    private double interferenceRange = 100.0;

    @Override
    public String getName() {
        return "UDGM Radio Medium";
    }

    public double getSuccessRatioTx() {
        return successRatioTx;
    }

    public void setSuccessRatioTx(double successRatioTx) {
        this.successRatioTx = successRatioTx;
    }

    public double getSuccessRatioRx() {
        return successRatioRx;
    }

    public void setSuccessRatioRx(double successRatioRx) {
        this.successRatioRx = successRatioRx;
    }

    public double getTransmissionRange() {
        return transmissionRange;
    }

    public void setTransmissionRange(double range) {
        this.transmissionRange = range;
    }

    public double getInterferenceRange() {
        return interferenceRange;
    }

    public void setInterferenceRange(double interferenceRange) {
        this.interferenceRange = interferenceRange;
    }

    public double getTxSuccessProbability(Node source) {
        return successRatioRx * source.getRadio().getTxProbability();
    }

    public double getRxSuccessProbability(Node source, Node dest) {
        double distance = source.getPosition().getDistance(dest.getPosition());
        double distanceSquared = Math.pow(distance, 2.0);
        double distanceMax = this.transmissionRange;
        if (distanceMax == 0.0) {
            return 0.0;
        }
        double distanceMaxSquared = Math.pow(distanceMax, 2.0);
        double ratio = distanceSquared / distanceMaxSquared;
        if (ratio > 1.0) {
            return 0.0;
        }
        ratio = 1.0 - ratio * (1.0 - successRatioRx);
        return ratio * dest.getRadio().getRxProbability();
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
