package se.sics.emul8.radiomedium;

import java.io.IOException;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.net.Server;
import se.sics.emul8.radiomedium.util.ArrayUtils;

public class Simulator {

    private static final Logger log = LoggerFactory.getLogger(Simulator.class);

    private long time;
    private Server server;
    private ClientConnection timeController = null;
    private ClientConnection[] emulators;
    private int emulatorsLeft = 0;
    private long lastTimeId = 0;
    
    /* this will have Niclas node stuff later? */
    private Hashtable<String, ClientConnection> nodes = new Hashtable();
    
    private void processAllEvents(long time) {
        /* process all the events in the event queue until time is time */
    }
    
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
        String replyStr = json.getString("reply", null);
        if (replyStr != null && replyStr.equals("OK")) {
            if (id == lastTimeId) {
                /* this *might* be the OK on the time-set request then... */
                if (client.setTime(time)) {
                    emulatorsLeft--;
                    if (emulatorsLeft == 0) {
                        System.out.println("No more emulators executing... - we are all at time:" + time);

                        /* this should be handled in other thread? */
                        processAllEvents(time);
                        try {
                            /* inform the time controller about the success */
                            timeController.send(new JsonObject().add("reply", "OK").add("id", lastTimeId));
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    reply = null; /* do not send a reply to this */
                } else {
                    /* ? some other thing happened ??? */
                    System.out.println("An emulator that did not progress time???");
                }
            }
        }
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
                    if (emulators == null) {
                        reply.add("reply","OK");
                    } else {
                        for (ClientConnection emu : emulators) {
                            emu.send(json);
                        }
                        /* handle the time elapsed here */
                        /* -- send a set-time to all with nodes and set the emulatorsLeft to the number of connected emulator */
                        /* -- collect all transmissions, and node/radio-changes in the event Q */
                        /* -- for each of the OK replies update the connection's time to the current and decrease emulatorsLeft */
                        /* -- when emulatorsLeft == 0 then process all events that are for this time period */
                        /* set the id to expect for the response */
                        lastTimeId = id;
                        emulatorsLeft = emulators.length;
                        reply = null; /* do not sent the reply now!!! */
                    }
                } catch(Exception e) {
                    reply.add("error", new JsonObject().add("desc", "failed to set time:" + e.getMessage()));
                }
            } else {
                reply.add("error", new JsonObject().add("desc", "only one time controller allowed"));
            }
        } else if (command.equals("transmit")) {
            /* Send along to all other nodes? */
        } else if (command.equals("node-config-set")) {
            /* add this client connection to the node emulator array */
            /* and possibly create a new node */
            JsonObject parms = json.get("params").asObject();
            String nodeId = "" + parms.get("node-id").asInt();
            if(nodes.get(nodeId) != null) {
                /* this node is already handled! */
                
            } else {
                boolean found = false;
                if (emulators != null) {
                    for (ClientConnection e : emulators) {
                        if (e == client) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    emulators = ArrayUtils.add(ClientConnection.class, emulators, client);
                }
                nodes.put(nodeId, client);
            }
        } else {
            /* What did we get here ??? */
            reply.add("error", new JsonObject().add("desc", "unhandled message."));
        }

        try {
            if (reply != null) {
                client.send(reply);
            }
        } catch (IOException e) {
            log.error("failed to reply to client", e);
        }
        return true;
    }
}
