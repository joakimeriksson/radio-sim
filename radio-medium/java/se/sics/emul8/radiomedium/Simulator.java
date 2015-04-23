package se.sics.emul8.radiomedium;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.net.Server;

public class Simulator {

    private static final Logger log = LoggerFactory.getLogger(Simulator.class);

    private long time;
    private Server server;
    private ClientConnection timeController = null;
    
    public void setServer(Server server) {
        this.server = server;
        time = 0;
    }

    public boolean handleMessage(ClientConnection client, JsonObject json) {
        JsonObject reply = new JsonObject();
        long id = -1;
        if ((id = json.getLong("id", -1)) != -1) {
            reply.set("id", id);
        }
        String command = json.getString("command", null);
        if (command == null) {
        } else if (command.equals("time-get")) {
            reply.add("reply",new JsonObject().add("time", time));
        } else if (command.equals("time-set")) {
            if (timeController == null) {
                timeController = client;
            }
            if (timeController == client){
                /* risky stuff ... */
                try {
                    time = ((JsonObject) json.get("params")).get("time").asLong();
                    reply.add("reply","OK");
                } catch(Exception e) {
                    reply.add("error", new JsonObject().add("desc", "failed to set time:" + e.getMessage()));
                }
            } else {
                reply.add("error", new JsonObject().add("desc", "only one time controller allowed"));
            }
        }

        try {
            client.send(reply);
        } catch (IOException e) {
            log.error("failed to reply to client", e);
        }
        return true;
    }
}
