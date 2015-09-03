package se.sics.emul8.radiomedium.util;

import java.io.IOException;

import se.sics.emul8.radiomedium.RadioListener;
import se.sics.emul8.radiomedium.RadioPacket;

public class PcapListener implements RadioListener{

    PcapExporter exporter;
    
    public PcapListener() {
        exporter = new PcapExporter();
    }
    
    @Override
    public void packetTransmission(RadioPacket packet) {
        try {
            exporter.exportPacketData(packet.getTime(), packet.getPacketDataAsBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
