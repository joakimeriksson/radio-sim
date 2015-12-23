package se.sics.emul8.radiomedium.net;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import se.sics.emul8.radiomedium.Node;
import se.sics.emul8.radiomedium.RadioMedium;
import se.sics.emul8.radiomedium.RadioPacket;
import se.sics.emul8.radiomedium.Simulator;

public class SimulatorJSONHandler {

    private static final Logger log = LoggerFactory.getLogger(SimulatorJSONHandler.class);

    private Simulator simulator;
    
    public SimulatorJSONHandler(Simulator simulator) {
        this.simulator = simulator;
    }

    public boolean handleMessage(ClientConnection client, JsonObject json) {
        JsonObject reply = new JsonObject();
        long id = -1;
        long time = simulator.getTime();
        ClientConnection[] emulators = simulator.getEmulators();
        if ((id = json.getLong("id", -1)) != -1) {
            reply.set("id", id);
        }
        String command = json.getString("command", null);
        String replyStr = json.getString("reply", null);
        if (replyStr != null && replyStr.equals("OK")) {
            simulator.emulatorTimeStepped(client, id, Simulator.TIME_STEP_OK);
            reply = null; /* no reply */
        }
        if (command == null) {

        } else if (command.equals("time-get")) {
            reply.add("reply",new JsonObject().add("time", time));
        } else if (command.equals("time-set")) {
            if (simulator.getTimeController() == null) {
                simulator.setTimeController(client);
            }
            if (simulator.getTimeController() == client){
                /* risky stuff ... */
                try {
                    time = json.get("params").asObject().get("time").asLong();
                    if (emulators == null) {
                        reply.add("reply","OK");
                    } else {
                        // Allocate new sequence number for the command
                        simulator.stepTime(time, id);
                        json = new JsonObject();
                        json.add("command", "time-set");
                        json.add("id", simulator.getLastTimeId());
                        json.add("params", new JsonObject().add("time", time));
                        
                        for (ClientConnection emu : emulators) {
                            emu.send(json);
                        }
                        /* handle the time elapsed here */
                        /* -- send a set-time to all with nodes and set the emulatorsLeft to the number of connected emulator */
                        /* -- collect all transmissions, and node/radio-changes in the event Q */
                        /* -- for each of the OK replies update the connection's time to the current and decrease emulatorsLeft */
                        /* -- when emulatorsLeft == 0 then process all events that are for this time period */
                        /* set the id to expect for the response */
                        reply = null; /* do not sent the reply now!!! */
                    }
                } catch(Exception e) {
                    reply.add("error", new JsonObject().add("desc", "failed to set time:" + e.getMessage()));
                }
            } else {
                reply.add("error", new JsonObject().add("desc", "only one time controller allowed"));
            }
        } else if (command.equals("transmit")) {
            String nodeId = json.get("source-node-id").toString();
            long tTime = json.get("time").asLong();
            String packetData = json.get("packet-data").asString();
            Node node = simulator.getNode(nodeId);
            RadioMedium medium = simulator.getRadioMedium();
            if (node == null) {
                log.error("non-existing node sending radio packet: {}", nodeId);
                reply.add("error", new JsonObject().add("desc", "could not find source node"));
            } else if (medium == null) {
                log.error("no radio medium available to transmitt radio packet");
                reply.add("error", new JsonObject().add("desc", "no radio medium available"));
            } else {
                RadioPacket packet = new RadioPacket(node, tTime, packetData);
                JsonValue value = json.get("transmit-power");
                if (value != null && value.isNumber()) {
                    packet.setTransmitPower(value.asDouble());
                }
                value = json.get("wireless-channel");
                if (value != null && value.isNumber()) {
                    packet.setWirelessChannel(value.asInt());
                }
                /* Send packet to listeners (visualizers, etc) and then to radio medium */
                simulator.notifyRadioListeners(packet);
                medium.transmit(packet);
                reply.add("reply", "OK");
            }

        } else if (command.equals("log")) {
            reply.add("reply", "OK");
            JsonObject params = json.get("params").asObject();
            String nodeId = "" + params.get("node-id").asInt();
            String logMsg = params.get("message").asString();
            Node node = simulator.getNode(nodeId);
            if(logMsg != null) {
                node.log(logMsg);
            }
            log.debug("Node:" + nodeId + " logMsg:" + logMsg);
        } else if (command.equals("node-config-set")) {
            /* add this client connection to the node emulator array */
            /* and possibly create a new node */
            JsonObject params = json.get("params").asObject();
            String nodeId = "" + params.get("node-id").asInt();
            Node node = simulator.addNode(nodeId, client);
            JsonValue value = params.get("position");
            if (value != null && value.isArray()) {
                JsonArray position = value.asArray();
                if (position.size() > 2) {
                    node.getPosition().set(position.get(0).asDouble(), position.get(1).asDouble(),
                            position.get(2).asDouble());
                } else if (position.size() > 1) {
                    node.getPosition().set(position.get(0).asDouble(), position.get(1).asDouble());
                }
            }
            value = params.get("transmit-power");
            if (value != null && value.isNumber()) {
                node.getRadio().setTransmitPower(value.asDouble());
            }
            value = params.get("wireless-channel");
            if (value != null && value.isNumber()) {
                node.getRadio().setWirelessChannel(value.asInt());
            }
            reply.add("reply", "OK");
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
