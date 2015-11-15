/*
 * Copyright (c) 2015, SICS Swedish ICT.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * \author
 *      Joakim Eriksson <joakime@sics.se> & Niclas Finne <nfi@sics.se>
 *
 * TODO: generalize all messages to not be based on a specific format.
 *
 */
package se.sics.emul8.radiomedium;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.net.Server;
import se.sics.emul8.radiomedium.util.ArrayUtils;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class Simulator {

    private static final Logger log = LoggerFactory.getLogger(Simulator.class);

    private static final Node[] NO_NODES = new Node[0];

    private RadioMedium radioMedium;
    
    private RadioListener[] radioListeners = null;
    
    private long time;
    private Server server;
    private ClientConnection timeController = null;
    private ClientConnection[] emulators;
    private int emulatorsLeft = 0;
    private long lastTimeId = -1;
    private long waitingForTimeId = -1;

    private AtomicLong simulatorMessageId = new AtomicLong();

    private ConcurrentHashMap<String, Node> nodeTable = new ConcurrentHashMap<String, Node>();
    private Node[] nodes = NO_NODES;

    public void addRadioListener(RadioListener listener) {
        radioListeners = ArrayUtils.add(RadioListener.class, radioListeners, listener);
    }
    
    private void notifyRadioListeners(RadioPacket packet) {
        RadioListener[] listeners = radioListeners;
        if (listeners != null) {
            for(int i = 0; i < listeners.length; i++) {
                listeners[i].packetTransmission(packet);
            }
        }
    }
    
    private void processAllEvents(long time) {
        /* process all the events in the event queue until time is time */
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
        time = 0;
    }

    public RadioMedium getRadioMedium() {
        return radioMedium;
    }

    public void setRadioMedium(RadioMedium radioMedium) {
        this.radioMedium = radioMedium;
    }

    public Node getNode(String id) {
        return this.nodeTable.get(id);
    }

    public Node[] getNodes() {
        return this.nodes;
    }

    protected Node addNode(String nodeId, ClientConnection client) {
        Node node;
        synchronized (this.nodeTable) {
            node = this.nodeTable.get(nodeId);
            if (node != null) {
                /* this node is already handled! */
                return node;
            }

            boolean found = false;
            if (this.emulators != null) {
                for (ClientConnection e : this.emulators) {
                    if (e == client) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                this.emulators = ArrayUtils.add(ClientConnection.class, this.emulators, client);
            }

            node = new Node(nodeId);
            node.setClientConnection(client);
            this.nodeTable.put(nodeId, node);
            this.nodes = ArrayUtils.add(Node.class, this.nodes, node);
        }
        return node;
    }

    protected Node removeNode(String nodeId) {
        Node node;
        synchronized (nodeTable) {
            node = nodeTable.get(nodeId);
            if (node == null) {
                /* The node is not registered */
                return null;
            }
            this.nodeTable.remove(nodeId);
            Node[] nodes = ArrayUtils.remove(this.nodes, node);
            if (nodes == null) {
                this.nodes = NO_NODES;
            } else {
                this.nodes = nodes;
            }

            // Should the emulator be removed when it no longer have any nodes?
            if (this.nodes != null && node.getClientConnection() != null) {
                boolean found = false;
                for (Node n : this.nodes) {
                    if (n.getClientConnection() == node.getClientConnection()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    this.emulators = ArrayUtils.remove(this.emulators, node.getClientConnection());
                }
            }
        }
        return node;
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
            if (id >= 0 && id == waitingForTimeId) {
                /* this *might* be the OK on the time-set request then... */
                if (emulatorsLeft <= 0) {
                    log.warn("unexpected time id reply: {}", waitingForTimeId);
                } else if (client.setTime(time)) {
                    emulatorsLeft--;
                    if (emulatorsLeft == 0) {
                        log.debug("No more emulators executing... - we are all at time: {}", time);
                        waitingForTimeId = -1;

                        /* this should be handled in other thread? */
                        processAllEvents(time);
                        try {
                            /* inform the time controller about the success */
                            timeController.send(new JsonObject().add("reply", "OK").add("id", lastTimeId));
                        } catch (IOException e) {
                            log.error("failed to acknowledge the time controller", e);
                        }
                    }
                    reply = null; /* do not send a reply to this */
                } else {
                    /* ? some other thing happened ??? */
                    log.warn("An emulator that did not progress time???");
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
                    time = json.get("params").asObject().get("time").asLong();
                    if (emulators == null) {
                        reply.add("reply","OK");
                    } else {
                        // Allocate new sequence number for the command
                        waitingForTimeId = simulatorMessageId.incrementAndGet();
                        lastTimeId = id;
                        emulatorsLeft = emulators.length;
                        json = new JsonObject();
                        json.add("command", "time-set");
                        json.add("id", lastTimeId);
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
            long time = json.get("time").asLong();
            String packetData = json.get("packet-data").asString();
            Node node = getNode(nodeId);
            RadioMedium medium = this.radioMedium;
            if (node == null) {
                log.error("non-existing node sending radio packet: {}", nodeId);
                reply.add("error", new JsonObject().add("desc", "could not find source node"));
            } else if (medium == null) {
                log.error("no radio medium available to transmitt radio packet");
                reply.add("error", new JsonObject().add("desc", "no radio medium available"));
            } else {
                RadioPacket packet = new RadioPacket(node, time, packetData);
                JsonValue value = json.get("transmit-power");
                if (value != null && value.isNumber()) {
                    packet.setTransmitPower(value.asDouble());
                }
                value = json.get("wireless-channel");
                if (value != null && value.isNumber()) {
                    packet.setWirelessChannel(value.asInt());
                }
                /* Send packet to listeners (visualizers, etc) and then to radio medium */
                notifyRadioListeners(packet);
                medium.transmit(packet);
                reply.add("reply", "OK");
            }

        } else if (command.equals("node-config-set")) {
            /* add this client connection to the node emulator array */
            /* and possibly create a new node */
            JsonObject params = json.get("params").asObject();
            String nodeId = "" + params.get("node-id").asInt();
            Node node = addNode(nodeId, client);
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
                node.setTransmitPower(value.asDouble());
            }
            value = params.get("wireless-channel");
            if (value != null && value.isNumber()) {
                node.setWirelessChannel(value.asInt());
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

    public void deliverRadioPacket(RadioPacket packet, Node destination, double rssi) {
        ClientConnection cc = destination.getClientConnection();
        if (cc == null || !cc.isConnected()) {
            // Can not send
            log.error("Node {} has no client connection", destination.getId());
            return;
        }
        JsonObject json = packet.toJsonDestination(destination, rssi);
        json.add("id", this.simulatorMessageId.incrementAndGet());
        try {
            cc.send(json);
        } catch (IOException e) {
            log.error("failed to deliver radio packet to node {}", destination.getId(), e);
        }
    }
}
