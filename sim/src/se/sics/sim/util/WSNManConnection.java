package se.sics.sim.util;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;

/**
 * Describe class WSNManConnection here.
 *
 *
 * Created: Thu May 10 17:08:15 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public final class WSNManConnection extends Visualizer implements Runnable {

    private static final boolean DEBUG = false;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean isOpen;

    /**
     * Creates a new <code>WSNManConnection</code> instance.
     *
     */
    public WSNManConnection(int port) throws Exception {
        socket = new Socket("localhost", port);
        writer = new PrintWriter(socket.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        isOpen = true;
        new Thread(this).start();

        send("network = createNetwork(1414);");
    }

    public void addNode(int node) {
        send("network.createNode(\"" + node + "\");");
    }

    public void setNodeProperty(int node, String name, String data) {
        send("network.createNode(\"" + node + "\").setProperty(\"" + name + "\",\"" + data + "\");");
    }

    public void setLocation(int node, int x, int y) {
        setNodeProperty(node, "location", "" + x + "," + y);
    }

    public void addLink(int node1, int node2) {
        send("node1 = network.createNode(\"" + node1 + "\");");
        send("node2 = network.createNode(\"" + node2 + "\");");
        send("node1.createLink(node2);");
    }

    public void setLinkProperty(int source, int destination, String name, String value) {
        if (value != null) {
            value = '"' + value + '"';
        }
        send("network.createNode(\"" + source + "\").createLink(network.createNode(\"" + destination
                + "\")).setProperty(\"" + name + "\"," + value + ");");
    }

    public void messageSent(int node) {
        long time = System.currentTimeMillis();
        send("network.getNode(\"" + node + "\").setProperty(\"lastsent.time\",\"" + time + "\");");
    }

    public void messageReceived(int node) {
        long time = System.currentTimeMillis();
        send("network.getNode(\"" + node + "\").setProperty(\"lastmessage.time\",\"" + time + "\");");
    }

    public void send(String text) {
        writer.println(text);
        writer.flush();
    }

    public void close() {
        try {
            if (isOpen) {
                isOpen = false;
                send("exit();");
                send("");
                writer.close();
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (DEBUG || !line.startsWith(" => ")) {
                    System.out.println("JSNetwork: " + line);
                }
            }
        } catch (Exception e) {
            if (isOpen) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        WSNManConnection wc = new WSNManConnection(14400);
        wc.addNode(1);
        wc.setNodeProperty(1, "location", "10,20");

        for (int i = 0, n = 100; i < n; i++) {
            try {
                wc.setNodeProperty(1, "location", "10," + i);
                Thread.sleep(100);
            } catch (Exception e) {
            }

        }
        wc.close();
    }
}
