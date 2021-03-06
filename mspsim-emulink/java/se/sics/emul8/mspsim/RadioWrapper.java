package se.sics.emul8.mspsim;

import se.sics.mspsim.chip.PacketListener;
import se.sics.mspsim.chip.RFListener;
import se.sics.mspsim.chip.Radio802154;

public class RadioWrapper implements RFListener {

    private final Radio802154 radio;
    private PacketListener packetListener;
    int len = 0;
    int pos = 0;
    byte[] buffer = new byte[128];

    public RadioWrapper(Radio802154 radio) {
        this.radio = radio;
        radio.addRFListener(this);
    }

    public synchronized void addPacketListener(PacketListener listener) {
        packetListener = PacketListener.Proxy.INSTANCE.add(packetListener, listener);
    }

    public synchronized void removePacketListener(PacketListener listener) {
        packetListener = PacketListener.Proxy.INSTANCE.remove(packetListener, listener);
    }

    public void packetReceived(byte[] receivedData) {
        System.out.println("WRadio: byte[] packet received: no-bytes:" + receivedData.length);
        // four zero bytes, 7a and then length...
        radio.receivedByte((byte)0);
        radio.receivedByte((byte)0);
        radio.receivedByte((byte)0);
        radio.receivedByte((byte)0);
        radio.receivedByte((byte)0x7a);
        // radio.receivedByte((byte) receivedData.length);

        for (int i = 0; i < receivedData.length; i++) {
            //            int data = receivedData[i];
            //            System.out.println("*** RF (external) Data :" + data + " = $" + Utils.hex8(data) + " => " +
            //                (char) data);

            radio.receivedByte(receivedData[i]);
        }
    }

    int getHexValue(char c) {
        if(c >= '0' && c <= '9') {
            return c - '0';
        }
        if(c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if(c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return 0;
    }
    
    public void packetReceived(String data) {
//        System.out.println("WRadio: Packet received: " + data);
        int len = data.length();
        byte[] packet = new byte[len / 2];
        for(int i = 0; i < len; i += 2) {
            packet[i / 2] =  (byte) ((getHexValue(data.charAt(i)) << 4) | (getHexValue(data.charAt(i + 1))));
//            System.out.printf("Data: %02x\n", packet[i/2] & 0xff);
        }
        packetReceived(packet);
    }

    // NOTE: len is not in the packet for now...
    public void receivedByte(byte data) {
        PacketListener listener = this.packetListener;
        //System.out.printf("*** RF Data :%d $%02x %c", data, data, (char)data);
        if (pos == 5) {
            len = data;
        }
        if (pos == 0) {
            if (listener != null) {
                listener.transmissionStarted();
            }
        }
        buffer[pos++] = data;
        // len + 1 = pos + 5 (preambles)
        if (len > 0 && len + 1 == pos - 5) {
            //            System.out.println("***** SENDING DATA from CC2420 len = " + len);
            byte[] packet = new byte[len + 1];
            System.arraycopy(buffer, 5, packet, 0, len + 1);
            if (listener != null) {
                listener.transmissionEnded(packet);
            }
            pos = 0;
            len = 0;
        }
    }
}
