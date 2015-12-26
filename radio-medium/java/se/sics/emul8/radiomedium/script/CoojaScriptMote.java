package se.sics.emul8.radiomedium.script;

import se.sics.emul8.radiomedium.Node;

public class CoojaScriptMote {

    public Node mote;
    public String id;
    public String lastMsg;  

    public void setMoteMsg(Node mote, String msg) {
        this.mote = mote;
        if (mote != null) {
            id = mote.getId();
        } else {
            id = "-1";
        }
        lastMsg = msg;
    }

    /* Data that should be input to an emulated node... */
    public void write(String data) {
        if (mote == null) return;
        System.out.println("**** " + data);
//        SerialPort serialPort = (SerialPort) mote.getInterfaces().getInterfaceOfType(Log.class);
//        serialPort.writeString(data);
    }
}
