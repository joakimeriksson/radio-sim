package se.sics.emul8.radiomedium;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.net.Server;

public class Simulator {

    private static final Logger log = LoggerFactory.getLogger(Simulator.class);

    private Server server;
    private int seqno;
    
    public void setServer(Server server) {
        this.server = server;
    }

    public boolean handleMessage(ClientConnection client, JsonObject json) {
        JsonObject reply = new JsonObject();
        String command = json.getString("command", null);
        if (command == null) {
        } else if (command.equals("time-get")) {
            reply.add("reply",new JsonObject().add("time", 0));
        }

        //reply.set("pong", ++seqno);
        try {
            client.send(reply);
        } catch (IOException e) {
            log.error("failed to reply to client", e);
        }
        return true;
    }
}
