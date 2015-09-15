package se.sics.emul8.radiomedium;

import se.sics.emul8.radiomedium.net.Server;
import se.sics.emul8.radiomedium.util.PcapListener;
import se.sics.emul8.web.WebServer;

public class Main {
    private static final int DEFAULT_PORT = 7711;


    public static void main(String[] args) throws InterruptedException {
        if (System.getProperty("logback.configurationFile") == null) {
            System.setProperty("logback.configurationFile", "logback.xml");
        }

        Simulator simulator = new Simulator();
        RadioMedium radioMedium = new NullRadioMedium();
        PcapListener pcapListener = new PcapListener();
        simulator.addRadioListener(pcapListener);
        radioMedium.setSimulator(simulator);
        simulator.setRadioMedium(radioMedium);
        Server server = new Server(DEFAULT_PORT);
        server.setSimulator(simulator);
        simulator.setServer(server);
        server.start();

        /* Quick hack to get a small web server running - for providing simulation info */

        WebServer ws = new WebServer();
        ws.setSimulator(simulator);
        try {
            ws.startWS();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        for (;;) {
            Thread.sleep(1000);
        }
    }

}
