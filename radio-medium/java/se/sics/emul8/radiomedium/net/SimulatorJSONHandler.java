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
        log.debug("Got: {}", json);
        String replyStatus = json.getString("reply", null);
        if (replyStatus != null) {
            return handleReply(client, json, replyStatus);
        }

        long id = json.getLong("id", -1);
        JsonObject reply = null;
        String command = json.getString("command", null);
        if (command == null) {
//          No command specified
            reply = createReplyError(id, "command-error", "no command specified");
        } else if (command.equals("time-get")) {
            if (id >= 0) {
                reply = createReplyObject(id).set("reply-object",new JsonObject().add("time", time));
            }
        } else if (command.equals("time-set")) {
            if (id < 0) {
                // time-set must include a reply id
                reply = createReplyError(id, "command-error", "time-set must include reply id");
            } else {
                if (simulator.getTimeController() == null) {
                    log.debug("{} set time controller", client.getName());
                    simulator.setTimeController(client);
                }
                if (simulator.getTimeController() == client){
                    /* risky stuff ... */
                    try {
                        time = json.get("parameters").asObject().get("time").asLong();
                        simulator.stepTime(time, id);
                    } catch(Exception e) {
                        reply = createReplyError(id, "command-error", "failed to set time:" + e.getMessage());
                    }
                } else {
                    reply = createReplyError(id, "command-error", "only one time controller allowed");
                }
            }
        } else if (command.equals("transmit")) {
            String nodeId = json.get("node-id").toString();
            long tTime = json.get("time").asLong();
            String packetData = json.getString("packet-data", "");
            Node node = simulator.getNode(nodeId);
            RadioMedium medium = simulator.getRadioMedium();
            if (node == null) {
                log.error("non-existing node sending radio packet: {}", nodeId);
                reply = createReplyError(id, "command-error", "could not find source node");
            } else if (medium == null) {
                log.error("no radio medium available to transmitt radio packet");
                reply = createReplyError(id, "command-error", "no radio medium available");
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
                } else {
                    // Illegal position
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
            value = params.get("rx-loss");
            if (value != null && value.isNumber()) {
                node.getRadio().setRxProbability(value.asDouble());
            }
            value = params.get("tx-loss");
            if (value != null && value.isNumber()) {
                node.getRadio().setTxProbability(value.asDouble());
            }
            value = params.get("radio-state");
            if (value != null && value.isString()) {
                String state = value.asString();
                node.getRadio().setEnabled(!"disabled".equals(state));
            }

            if (id >= 0) {
                // Add node-info
                JsonObject nodeInfo = new JsonObject();
                nodeInfo.add("node_id", nodeId);
                nodeInfo.add("rssi", node.getRadio().getRSSI());
                nodeInfo.add("receiving", node.getRadio().getReceivingState());
                nodeInfo.add("wireless-channel", node.getRadio().getWirelessChannel());
                reply = createReplyObject(id).set("reply-object", new JsonObject().add("node-info", nodeInfo));
            }

        } else if (command.equals("link-quality")) {
            JsonObject link = json.get("link").asObject();
            String sourceId = link.get("src").toString();
            String destId = link.get("dst").toString();
            JsonValue value = json.get("wireless-channel");
            int channel = -1;
            if (value != null && value.isNumber()) {
                channel = value.asInt();
            }
            value = link.get("quality");
            if (value != null && value.isNumber()) {
                // TODO update radio medium
                log.debug("LINK {} => {} PRR {} (channel {})", sourceId, destId, value.asInt(), channel);
            }

        } else if (command.equals("configuration-set")) {
            if (simulator.getTimeController() != null) {
                reply = createReplyError(id, "command-error", "already initialized");
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
//                value = params.get("data");
            }
        } else if (command.equals("subscribe-event")) {
            log.debug("Adding client connection as event listener");
            simulator.addEventListener(client);
        } else if (command.equals("unsubscribe-event")) {
            log.debug("Removed client connection as event listener");
            simulator.removeEventListener(client);
        } else {
            /* What did we get here ??? */
            reply = createReplyError(id, "command-error", "unsupported command: " + command);
        }

        if (reply == null && id >= 0) {
            // A reply is expected
            reply = createReplyObject(id).set("reply-object", new JsonObject());
        }

        if (reply != null) {
            try {
                /* Send a JSON reply - hopefully it is JSON */
                ((JSONClientConnection) client).send(reply);
            } catch (IOException e) {
                log.error("failed to reply to client", e);
            }
        }
        return true;
    }

    private boolean handleReply(ClientConnection client, JsonObject json, String replyStatus) {
        long id = json.getLong("id", -1);
        if ("OK".equals(replyStatus)) {
            if (id >= 0 && id == simulator.getWaitingForTimeId()) {
                simulator.emulatorTimeStepped(client, id, Simulator.TIME_STEP_OK);
            }
            return true;
        }
        log.error("{} error reply: {}", client.getName(), json);
        return true;
    }

    private JsonObject createReplyObject(long id) {
        JsonObject reply = new JsonObject();
        if (id >= 0) {
            reply.set("id", id);
        }
        reply.set("reply", "OK");
        return reply;
    }

    private JsonObject createReplyError(long id, String errorClass, String description) {
        JsonObject reply = new JsonObject();
        if (id >= 0) {
            reply.set("id", id);
        }
        reply.set("reply", "error");
        reply.set("reply-object", new JsonObject().add("class", errorClass).add("description", description));
        return reply;
    }
}
