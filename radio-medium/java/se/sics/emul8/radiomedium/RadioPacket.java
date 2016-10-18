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
package se.sics.emul8.radiomedium;

import javax.xml.bind.DatatypeConverter;
import com.eclipsesource.json.JsonObject;

public class RadioPacket {

    private Node node;
    private long time;
    private double txpower;
    private int channel;
    private String packetData;

    public RadioPacket(Node node, long time, String packetData) {
        this.node = node;
        this.time = time;
        this.txpower = node.getRadio().getTransmitPower();
        this.channel = node.getRadio().getWirelessChannel();
        this.packetData = packetData;
    }

    public Node getSource() {
        return this.node;
    }

    public long getStartTime() {
        return this.time;
    }

    public long getEndTime() {
        return this.time + getPacketAirTime();
    }
    
    /* This is assumed to be 802.15.4 250 kbps for now */
    public long getPacketAirTime() {
        if (packetData != null) {
            //System.out.println("Packet size:" + packetData.length() + " usec:" + 32 * packetData.length());
            /* microseconds delay for the packet - 250 000 bit / s = 31 000 byte / s = 31 byte / ms =>
             * each byte takes 1000 us / 31 byte = 32 us per byte */
            return packetData.length() * 32;
        }
        return 0;
    }

    public double getTransmitPower() {
        return this.txpower;
    }

    public void setTransmitPower(double txpower) {
        this.txpower = txpower;
    }

    public int getWirelessChannel() {
        return this.channel;
    }

    public void setWirelessChannel(int channel) {
        this.channel = channel;
    }

    public String getPacketDataAsHex() {
        return packetData;
    }
    
    public byte[] getPacketDataAsBytes() {
        return DatatypeConverter.parseHexBinary(packetData);
    }

    public JsonObject toJsonDestination(Node destination, double rssi) {
        JsonObject json = new JsonObject();
        json.add("command", "receive");
        json.add("node-id", destination.getId());
        json.add("time", getStartTime());
        json.add("rf-power", rssi);
        json.add("wireless-channel", this.channel);
        json.add("packet-data", this.packetData);
        return json;
    }

}
