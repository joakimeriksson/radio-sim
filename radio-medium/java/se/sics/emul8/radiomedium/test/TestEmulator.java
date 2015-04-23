package se.sics.emul8.radiomedium.test;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.net.ClientHandler;
import com.eclipsesource.json.JsonObject;

public class TestEmulator implements ClientHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final ClientConnection clientConnection;
    private int nodeId;
    private long myTime;

    public TestEmulator(String host, int port) throws IOException {
        this.clientConnection = new ClientConnection(this, host, port);
        this.clientConnection.start();
        nodeId = (int) (Math.random() * 10);
    }

    public ClientConnection getConnection() {
        return this.clientConnection;
    }

    private JsonObject createCommand(String cmd, JsonObject params) {
        JsonObject jsonCmd = new JsonObject().add("command",  cmd).add("params", params);
        return jsonCmd;
    }
    
    private void serveForever() {
        JsonObject reqNode = createCommand("node-config-set", new JsonObject().add("node-id", nodeId));
        JsonObject transmit = new JsonObject();
        transmit.add("command", "transmit");
        transmit.add("source-node-id", nodeId);
        transmit.add("packet-data", "0102030405");
        try {
            clientConnection.send(reqNode);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        while (true) {
            try {
                Thread.sleep(5000);
                transmit.set("time", myTime);
                clientConnection.send(transmit);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean handleMessage(ClientConnection clientConnection, JsonObject json) {
        System.out.println("RECV: " + json);
        String cmd = json.getString("command", null);
        JsonObject reply = new JsonObject();
        long id = 0;
        if ((id = json.getLong("id", -1)) != -1) {
            reply.set("id", id);
        }

        if (cmd != null) {
            if (cmd.equals("time-set")) {
                JsonObject params = (JsonObject) json.get("params"); 
                myTime = params.getLong("time", 0);
                System.out.println("Accepting time elapsed." + myTime);
                reply.set("reply", "OK");
                try {
                    clientConnection.send(reply);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (cmd.equals("transmit")) {
                String destId = json.getString("destination-node-id", null);
                System.out.println("Transmission for node: " + destId);
            }
        }
        return true;
    }

    @Override
    public void clientClosed(ClientConnection clientConnection) {
        log.error("Radio medium connection closed!");
        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        TestEmulator c = new TestEmulator("127.0.0.1", 7711);
        c.serveForever();
    }

}
