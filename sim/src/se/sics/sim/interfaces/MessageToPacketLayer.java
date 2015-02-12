package se.sics.sim.interfaces;

public interface MessageToPacketLayer {

    public static final int RADIO_PACKET_SIZE = 85; // summa cirka 85 varav 64
                                                    // appl.data
    public static final int APPLICATION_PACKET_SIZE = 64;
    public static final int PROTOCOL_OVERHEAD = RADIO_PACKET_SIZE - APPLICATION_PACKET_SIZE;
    public static final int MESSAGE_LENGTH = 32 * 1024; // 4096; //32*1024; //i
                                                        // bytes. 32KB

    public static final double IDEAL_TIME_UNTIL_PACKET_ACC = RadioNodeInterface.RADIO_INIT_TRANSMIT_TIME
            + RadioNodeInterface.TRANSMIT_STARTUP_DELAY
            + (RadioNodeInterface.TRANSMIT_DELAY + RadioNodeInterface.PREAMBLE_SIZE
                    * RadioNodeInterface.MICROSEC_PER_BYTE_SENDING)
            + RadioNodeInterface.TRANSMIT_STARTUP_DELAY
            + (RadioNodeInterface.TRANSMIT_DELAY + RadioNodeInterface.PREAMBLE_ACK_SIZE
                    * RadioNodeInterface.MICROSEC_PER_BYTE_SENDING)
            + RadioNodeInterface.TRANSMIT_STARTUP_DELAY
            + (RadioNodeInterface.TRANSMIT_DELAY + MessageToPacketLayer.RADIO_PACKET_SIZE
                    * RadioNodeInterface.MICROSEC_PER_BYTE_SENDING)
            + RadioNodeInterface.TRANSMIT_STARTUP_DELAY
            + (RadioNodeInterface.TRANSMIT_DELAY + RadioNodeInterface.MESSAGE_ACK_SIZE
                    * RadioNodeInterface.MICROSEC_PER_BYTE_SENDING);

    public static final double COPY_TIME = MessageToPacketLayer.APPLICATION_PACKET_SIZE
            * RadioNodeInterface.MICROSEC_PER_BYTE_COPYING;

    public static final double IDEAL_PACKET_SEND_INTERVAL = IDEAL_TIME_UNTIL_PACKET_ACC + COPY_TIME;
    public static final int MAX_PACKET_SEND_INTERVAL = (int) (2 * (IDEAL_PACKET_SEND_INTERVAL + MacLayer.SLEEP_TIME_DEFAULT));

    public static final int MAX_TIME_TO_TRANSFER_ONE_MESSAGE = (MESSAGE_LENGTH / APPLICATION_PACKET_SIZE)
            * MAX_PACKET_SEND_INTERVAL;

    public void transmissionComplete(long time);

    public void transmissionFailed(long time);

    public long getActiveMessageSendTime();

    public int getMessagesSentCount();

    public int getMessagesReceivedCount();

    public long getPacketSendInterval();
}
