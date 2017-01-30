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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.botbox.scheduler.EventQueue;
import com.botbox.scheduler.TimeEvent;
import se.sics.emul8.radiomedium.events.ReceptionEvent;
import se.sics.emul8.radiomedium.events.TransmissionEvent;
import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.util.ArrayUtils;

public class Simulator {

    private static final Logger log = LoggerFactory.getLogger(Simulator.class);
    public static final int DEFAULT_PORT = 7711; /* for the JSON protocol */
    
    private static final Node[] NO_NODES = new Node[0];

    private EventQueue eventQueue = new EventQueue();
    private Object eventLock = new Object();
    
    /* Responses, etc from emulators */
    public static final long TIME_STEP_OK = 0;

    private RadioMedium radioMedium;
    private final Random simulationRandom;

    private RadioListener[] radioListeners = null;
    
    private long currentTime;
    private long stepTime;
    private ClientConnection timeController = null;
    private ClientConnection[] emulators;
    private ClientConnection[] eventListeners;
    private int emulatorsLeft = 0;
    private long timeControllerLastTimeId = -1;
    private long waitingForTimeId = -1;

    private AtomicLong simulatorMessageId = new AtomicLong(1000);

    private ConcurrentHashMap<String, Node> nodeTable = new ConcurrentHashMap<String, Node>();
    private Node[] nodes = NO_NODES;

    public Simulator() {
        this(new Random());
    }

    public Simulator(Random r) {
        this.simulationRandom = r;
    }

    public Random getRandom() {
        return this.simulationRandom;
    }

    public boolean isWaitingForTimeStep() {
        return waitingForTimeId >= 0;
    }

    public long getWaitingForTimeId() {
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

    public long getNextMessageId() {
      return simulatorMessageId.updateAndGet(unsignedIncrementOperator);
    }

    public synchronized void addEventListener(ClientConnection client) {
        this.eventListeners = ArrayUtils.add(ClientConnection.class, this.eventListeners, client);
    }

    public synchronized void removeEventListener(ClientConnection client) {
        this.eventListeners = ArrayUtils.remove(this.eventListeners, client);
    }

    public ClientConnection[] getEmulators() {
        return emulators;
    }

    public void emulatorTimeStepped(ClientConnection client, long id, long timeStepOk) {
//        log.debug("Got time stepped to " + time + " id:"  + id + " OK:" + timeStepOk + " waitingFor:" + waitingForTimeId + " Emu:" + emulatorsLeft);
        if (this.waitingForTimeId < 0) {
            // Not waiting for a time step to finish
        } else if(id == this.waitingForTimeId) {
            if (client.setTime(stepTime, id)) {
                emulatorsLeft--;
            } else {
                /* What to do here? */
                log.warn("ClientConnection did not accept time stepped call");
            }

            if (getEmulatorsLeft() == 0) {
                emulatorTimeStepDone();
            }
        } else {
            // This is not the id we are waiting for
            log.debug("waiting for Wrong ID for emulatorTimeStepped:{} got {}", waitingForTimeId, id);
        }
    }

    private void emulatorTimeStepDone() {
        this.currentTime = stepTime;
        this.waitingForTimeId = -1;

        /* this should be handled in other thread? */
        log.debug("timeStep finished - processing all events");
        processAllEvents(currentTime);

        // Notify the time controller that we now have stepped to the specified time.
        getTimeController().timeStepDone(this.timeControllerLastTimeId);
    }

    public int getEmulatorsLeft() {
        return emulatorsLeft;
    }

    public void stepTime(long time, long id) {
        ClientConnection[] em = emulators;
        if (emulatorsLeft > 0) {
            log.warn("*** still waiting for {} clients when stepping time again to {}", emulatorsLeft, time);
        }
        waitingForTimeId = getNextMessageId();
        timeControllerLastTimeId = id;
        if (stepTime > time) {
            log.warn("*** trying to step time backwards from {} to {}", stepTime, time);
        }
        stepTime = time;

        if (emulators == null) {
            emulatorsLeft = 0;
            emulatorTimeStepDone();
            return;
        }
        emulatorsLeft = emulators.length;

        /* inform all emulators about the time stepping */
        for (int i = 0; i < em.length; i++) {
            em[i].emulateToTime(getNodes(), time, waitingForTimeId);
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

    private TimeEvent nextEvent(long time) {
        synchronized (eventLock) {
            long nextTime = eventQueue.nextTime();
            if (nextTime >= 0 && nextTime < time) {
                return eventQueue.popFirst();
            }
            return null;
        }
    }

    /* process all the events in the event queue until time is time */
    private void processAllEvents(long time) {
        for(TimeEvent e = nextEvent(time); e != null; e = nextEvent(time)) {
            e.execute(time);
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
 
    public void generateReceptionEvents(RadioPacket packet, Node destination, double rssi) {
        synchronized (eventLock) {
            long packetTime = packet.getStartTime();
            if (packetTime < currentTime) {
                packetTime = currentTime;
            }
            ReceptionEvent teStart =
                    new ReceptionEvent(packetTime, this, packet, destination, rssi, true);
            ReceptionEvent teEnd =
                    new ReceptionEvent(packetTime + packet.getPacketAirTime(), this, packet, destination, rssi, false);
            eventQueue.addEvent(teStart);
            eventQueue.addEvent(teEnd);
        }
    }

    public void generateTransmissionEvents(RadioPacket packet) {
        synchronized (eventLock) {
            long packetTime = packet.getStartTime();
            if (packetTime < currentTime) {
                packetTime = currentTime;
            }
            TransmissionEvent teStart =
                    new TransmissionEvent(packetTime, this, packet, true);
            TransmissionEvent teEnd =
                    new TransmissionEvent(packetTime + packet.getPacketAirTime(), this, packet, false);
            eventQueue.addEvent(teStart);
            eventQueue.addEvent(teEnd);
        }
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
        cc.sendPacket(packet, destination, -1, rssi);
    }

    private static final LongUnaryOperator unsignedIncrementOperator = new LongUnaryOperator() {

        @Override public long applyAsLong(long value) {
            return value == Long.MAX_VALUE ? 0 : (value + 1);
        }

    };

}
