package se.sics.emul8.radiomedium.test;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.net.ClientHandler;
import com.eclipsesource.json.JsonObject;

public class TestClient implements ClientHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final ClientConnection clientConnection;

    public TestClient(String host, int port) throws IOException {
        this.clientConnection = new ClientConnection(this, host, port);
        this.clientConnection.start();
    }

    public ClientConnection getConnection() {
        return this.clientConnection;
    }

    private void serveForever() {
        while (true) {
            JsonObject timeReq = new JsonObject();
            timeReq.add("command", "time-get");
            
            try {
                Thread.sleep(5000);
                
                clientConnection.send(timeReq);
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
        return true;
    }

    @Override
    public void clientClosed(ClientConnection clientConnection) {
        log.error("Radio medium connection closed!");
        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        TestClient c = new TestClient("127.0.0.1", 7711);
        c.serveForever();
    }

}
