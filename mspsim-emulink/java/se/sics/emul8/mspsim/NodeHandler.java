package se.sics.emul8.mspsim;

import java.io.IOException;

import se.sics.emul8.radiomedium.Position;
import se.sics.mspsim.chip.PacketListener;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
import se.sics.mspsim.platform.GenericNode;

public class NodeHandler implements USARTListener, PacketListener {

    private EmuLink emuLink;
    private int nodeId;
    private Position position;
    private GenericNode node;
    private RadioWrapper wradio;
    
    
    public NodeHandler(int nodeId, EmuLink link, GenericNode node, RadioWrapper radio) {
        emuLink = link;
        this.nodeId = nodeId;
        this.node = node;
        wradio = radio;
        position = new Position();
    }
    
    public GenericNode getNode() {
        return node;
    }
    
    public Position getPosition() {
        return position;
    }
    
    
    @Override
    public void transmissionEnded(byte[] data) {
        System.out.println("*** Packet to send: " + data.length + " bytes");
        try {
            emuLink.sendPacket(nodeId, data);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void transmissionStarted() {
        System.out.println("*** Transmission started...");
    }
 
    /* UART Handling */
    static StringBuffer sbuf = new StringBuffer();
    @Override
    public void dataReceived(USARTSource source, int data) {
        // Receive and printout UART data. 
        sbuf.append((char) data);
        if(data == '\n') {
            System.out.println("UART Data:" + sbuf.toString());
            try {
                emuLink.sendLog(nodeId, sbuf.toString());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            sbuf.setLength(0);
        }
    }

    public void packetReceived(String data) {
        wradio.packetReceived(data);
    }    

}
