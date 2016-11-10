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
 */
package se.sics.emul8.radiomedium.net;
import java.util.Hashtable;
import se.sics.emul8.radiomedium.Node;
import se.sics.emul8.radiomedium.RadioPacket;
import se.sics.emul8.radiomedium.SimulationEvent;

public abstract class ClientConnection {

    private Hashtable<String, Object> clientProperties = new Hashtable<String, Object>();

    private boolean isConnected = false;
    private String name;

    protected ClientConnection(String name) {
        this.name = name;
    }

    public void setProperty(String name, Object value) {
        clientProperties.put(name, value);
    }
    
    public Object getProperty(String name) {
        return clientProperties.get(name);
    }
    
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public boolean isConnected() {
        return isConnected;
    }

    protected void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public abstract void close();
    
    /* Called when it is time to step time for all emulators. */
    public abstract void emulateToTime(Node[] nodes, long time, long timeId);

    public abstract long getTime();

    /* Called when it is time set the time to the higher value (needed?) */
    public abstract boolean setTime(long time, long timeId);
    
    /* Called when the time in the simulator has stepped to the desired time - when this requested the time-step */
    public abstract void timeStepDone(long timeId);

    /* Sent a packet to some node which this connection is responsible for */
    public abstract void sendPacket(RadioPacket packet, Node destination, long id, double rssi);
    
    /* An event that this is subscriber of */
    public abstract void sendEvent(SimulationEvent event);
}
