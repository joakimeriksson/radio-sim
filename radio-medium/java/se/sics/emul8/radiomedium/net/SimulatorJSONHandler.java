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
        long time = simulator.getTime();
        ClientConnection[] emulators = simulator.getEmulators();
        log.debug("Got: {}", json);
        long id = json.getLong("id", -1);
        JsonObject reply = createReplyObject(id);
        String command = json.getString("command", null);
        String replyStr = json.getString("reply", null);
        /* Assumed to be a time-set reply ???*/
        if (replyStr != null && replyStr.equals("OK")) {
            simulator.emulatorTimeStepped(client, id, Simulator.TIME_STEP_OK);
            reply = null; /* no reply */
        }
        if (command == null) {

        } else if (command.equals("time-get")) {
            reply.add("reply-object",new JsonObject().add("time", time));
        } else if (command.equals("time-set")) {
            if (simulator.getTimeController() == null) {
                simulator.setTimeController(client);
            }
            if (simulator.getTimeController() == client){
                /* risky stuff ... */
                try {
                    time = json.get("parameters").asObject().get("time").asLong();
                    if (emulators != null) {
                        // Allocate new sequence number for the command
                        simulator.stepTime(time, id);
//                        json = new JsonObject();
//                        json.add("command", "time-set");
//                        json.add("id", simulator.getLastTimeId());
//                        json.add("parameters", new JsonObject().add("time", time));
//                        
//                        for (ClientConnection emu : emulators) {
//                            emu.send(json);
//                        }
                        /* handle the time elapsed here */
                        /* -- send a set-time to all with nodes and set the emulatorsLeft to the number of connected emulator */
                        /* -- collect all transmissions, and node/radio-changes in the event Q */
                        /* -- for each of the OK replies update the connection's time to the current and decrease emulatorsLeft */
                        /* -- when emulatorsLeft == 0 then process all events that are for this time period */
                        /* set the id to expect for the response */
                        reply = null; /* do not sent the reply now!!! */
                    }
                } catch(Exception e) {
                    setReplyError(reply, "command-error", "failed to set time:" + e.getMessage());
                }
            } else {
                setReplyError(reply, "command-error", "only one time controller allowed");
            }
        } else if (command.equals("transmit")) {
            String nodeId = json.get("node-id").toString();
            long tTime = json.get("time").asLong();
            String packetData = json.get("packet-data").asString();
            Node node = simulator.getNode(nodeId);
            RadioMedium medium = simulator.getRadioMedium();
            if (node == null) {
                log.error("non-existing node sending radio packet: {}", nodeId);
                setReplyError(reply, "command-error", "could not find source node");
            } else if (medium == null) {
                log.error("no radio medium available to transmitt radio packet");
                setReplyError(reply, "command-error", "no radio medium available");
            } else {
                RadioPacket packet = new RadioPacket(node, tTime, packetData);
                JsonValue value = json.get("rf-power");
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
            }

        } else if (command.equals("log")) {
            JsonObject params = json.get("parameters").asObject();
            String nodeId = params.get("node-id").toString();
            String logMsg = params.get("message").asString();
            Node node = simulator.getNode(nodeId);
            if(logMsg != null) {
                node.log(logMsg);
            }
            log.debug("Node:" + nodeId + " logMsg:" + logMsg);
        } else if (command.equals("node-config-set")) {
            /* add this client connection to the node emulator array */
            /* and possibly create a new node */
            JsonObject params = json.get("parameters").asObject();
            String nodeId = params.get("node-id").toString();
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
            value = params.get("rf-power");
            if (value != null && value.isNumber()) {
                node.getRadio().setTransmitPower(value.asDouble());
            }
            value = params.get("wireless-channel");
            if (value != null && value.isNumber()) {
                node.getRadio().setWirelessChannel(value.asInt());
            }
            value = params.get("radio-state");
            if (value != null && value.isString()) {
                String state = value.asString();
                node.getRadio().setEnabled(!"disabled".equals(state));
            }
        } else if (command.equals("configuration-set")) {
            if (simulator.getTimeController() != null) {
                setReplyError(reply, "command-error", "already initialized");
            } else {
                JsonObject params = json.get("parameters").asObject();
                JsonValue value = params.get("wireless-standard");
                if (value != null) {
                    log.debug("CONFIG: wireless-standard: {}", value);
                }
                value = params.get("propagation-option");
                if (value != null) {
                    log.debug("CONFIG: propagation-option: {}", value);
                }
//                value = params.get("matrix-data");
            }
        } else if (command.equals("subscribe-event")) {
            log.debug("Adding clienct connection as event listener");
            simulator.addEventListener(client);
        } else {
            /* What did we get here ??? */
            setReplyError(reply, "command-error", "unsupported command: " + command);
        }

        try {
            /* Send a JSON reply - hopefully it is JSON */
            if (reply != null) {
                ((JSONClientConnection) client).send(reply);
            }
        } catch (IOException e) {
            log.error("failed to reply to client", e);
        }
        return true;
    }

    private JsonObject createReplyObject(long id) {
        JsonObject reply = new JsonObject();
        if (id >= 0) {
            reply.set("id", id);
        }
        reply.set("reply", "OK");
        reply.set("reply-object", new JsonObject());
        return reply;
    }

    private void setReplyError(JsonObject reply, String errorClass, String description) {
        reply.set("reply", "error");
        reply.set("reply-object", new JsonObject().add("class", errorClass).add("description", description));
    }
}
