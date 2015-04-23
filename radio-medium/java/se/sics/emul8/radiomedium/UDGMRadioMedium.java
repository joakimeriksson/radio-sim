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

    @Override
    public void transmit(RadioPacket packet) {
        // TODO Auto-generated method stub

    }

}
