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

    public TestEmulator(String host, int port) throws IOException {
        this.clientConnection = new ClientConnection(this, host, port);
        this.clientConnection.start();
    }

    public ClientConnection getConnection() {
        return this.clientConnection;
    }

    private JsonObject createCommand(String cmd, JsonObject params) {
        JsonObject jsonCmd = new JsonObject().add("command",  cmd).add("params", params);
        return jsonCmd;
    }
    
    private void serveForever() {
        long time = 0;
        JsonObject reqNode = createCommand("node-config-set", new JsonObject().add("node-id", (int) (Math.random() * 10)));
        try {
            clientConnection.send(reqNode);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
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

        if (cmd != null && cmd.equals("time-set")) {
            System.out.println("Accepting time elapsed...");
            reply.set("reply", "OK");
            try {
                clientConnection.send(reply);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
