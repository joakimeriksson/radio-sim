package se.sics.emul8.client;

import java.io.IOException;
import com.eclipsesource.json.JsonObject;

public class Main {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 7711;

    public static void main(String[] args) throws InterruptedException, IOException {
        if (System.getProperty("logback.configurationFile") == null) {
            System.setProperty("logback.configurationFile", "logback.xml");
        }

        Client client = new Client(DEFAULT_HOST, DEFAULT_PORT);
        client.start();

        JsonObject json = new JsonObject();
        int seqno = 0;
        while (client.isConnected()) {
            json.set("ping", ++seqno);
            client.send(json);
            Thread.sleep(1000);
        }
    }

}
