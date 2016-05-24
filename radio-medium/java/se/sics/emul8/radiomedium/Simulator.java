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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.botbox.scheduler.EventQueue;
import com.botbox.scheduler.TimeEvent;

import se.sics.emul8.radiomedium.events.TransmissionEvent;
import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.util.ArrayUtils;

public class Simulator {

    private static final Logger log = LoggerFactory.getLogger(Simulator.class);
    public static final int DEFAULT_PORT = 7711; /* for the JSON protocol */
    
    private static final Node[] NO_NODES = new Node[0];

    private EventQueue eventQueue = new EventQueue();
    
    /* Responses, etc from emulators */
    public static final long TIME_STEP_OK = 0;

    private RadioMedium radioMedium;

    private RadioListener[] radioListeners = null;
    
    private long currentTime;
    private long stepTime;
    private ClientConnection timeController = null;
    private ClientConnection[] emulators;
    private ClientConnection[] eventListeners;
    private int emulatorsLeft = 0;
    private long timeControllerLastTimeId = -1;
    private long waitingForTimeId = -1;

    private AtomicLong simulatorMessageId = new AtomicLong();

    private ConcurrentHashMap<String, Node> nodeTable = new ConcurrentHashMap<String, Node>();
    private Node[] nodes = NO_NODES;

    public long getWatitingForTimeId() {
        return waitingForTimeId;
    }

    public ClientConnection getTimeController() {
        return timeController;
    }

    public void setTimeController(ClientConnection client) {
        timeController = client;
    }

    public long getTime() {
        return currentTime;
    }
    
    public void addEventListener(ClientConnection client) {
        this.eventListeners = ArrayUtils.add(ClientConnection.class, this.eventListeners, client);
    }

    public ClientConnection[] getEmulators() {
        return emulators;
    }

    public void emulatorTimeStepped(ClientConnection client, long id, long timeStepOk) {
//        log.debug("Got time stepped to " + time + " id:"  + id + " OK:" + timeStepOk + " waitingFor:" + waitingForTimeId + " Emu:" + emulatorsLeft);
        if(id == this.waitingForTimeId) {
            if (client.setTime(stepTime, id)) {
                emulatorsLeft--;
                System.out.print("" + emulatorsLeft);
            } else {
              /* What to do here? */
                log.debug("ClientConnection did not accept time stepped call");
            }

            if (getEmulatorsLeft() == 0) {
                currentTime = stepTime;
//                log.debug("No more emulators executing... - we are all at time: {}", time);
                /* this should be handled in other thread? */
                processAllEvents(currentTime);
                getTimeController().timeStepDone(this.timeControllerLastTimeId);
            }
        } else {
            log.debug("Wrong ID for emulatorTimeStepped:{} got {}", waitingForTimeId, id);
        }
    }

    public int getEmulatorsLeft() {
        return emulatorsLeft;
    }

    public void stepTime(long time, long id) {
        waitingForTimeId = simulatorMessageId.incrementAndGet();
        timeControllerLastTimeId = id;
        if (emulators == null) {
            emulatorsLeft = 0;
            return;
        }
        stepTime = time;
        emulatorsLeft = emulators.length;

        /* inform all emulators about the time stepping */
        ClientConnection[] em = emulators;
        for (int i = 0; i < em.length; i++) {
            em[i].emulateToTime(time, waitingForTimeId);
        }
    }

    public long getLastTimeIdFromTimeController() {
        return timeControllerLastTimeId;
    }

    public void addRadioListener(RadioListener listener) {
        radioListeners = ArrayUtils.add(RadioListener.class, radioListeners, listener);
    }
    
    public void notifyRadioListeners(RadioPacket packet) {
        RadioListener[] listeners = radioListeners;
        if (listeners != null) {
            for(int i = 0; i < listeners.length; i++) {
                listeners[i].packetTransmission(packet);
            }
        }
    }

    /* process all the events in the event queue until time is time */
    private void processAllEvents(long time) {
        long nextTime = 0;
        while ((nextTime = eventQueue.nextTime()) != -1 && nextTime < time) {
            eventQueue.popFirst().execute(time);
        }
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

    public Node addNode(String nodeId, ClientConnection client) {
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

            node = new Node(nodeId, this);
            node.setClientConnection(client);
            this.nodeTable.put(nodeId, node);
            this.nodes = ArrayUtils.add(Node.class, this.nodes, node);
        }
        return node;
    }

    public Node removeNode(String nodeId) {
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

    public void deliverEvent(SimulationEvent event) {
        ClientConnection[] listeners = eventListeners;
        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].sendEvent(event);
            }
        }
    }
 
    public void generateTransmissionEvents(RadioPacket packet, Node destination, double rssi) {
        TransmissionEvent teStart = new TransmissionEvent(time, this, packet, destination, rssi, true);
        TransmissionEvent teEnd;
        
        teEnd = new TransmissionEvent(time + packet.getPacketAirTime(), this, packet, destination, rssi, true);
        eventQueue.addEvent(teStart);
        eventQueue.addEvent(teEnd);
    }
    
    /*  
     * Deliver a radio packet to a specific destination node.
     * 
    */
    public void deliverRadioPacket(RadioPacket packet, Node destination, double rssi) {
        ClientConnection cc = destination.getClientConnection();
        if (cc == null || !cc.isConnected()) {
            // Can not send
            log.error("Node {} has no client connection", destination.getId());
            return;
        }
        cc.sendPacket(packet, destination, this.simulatorMessageId.incrementAndGet(), rssi);
    }


}
