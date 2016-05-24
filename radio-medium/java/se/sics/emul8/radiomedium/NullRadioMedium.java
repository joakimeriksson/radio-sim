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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullRadioMedium extends AbstractRadioMedium {

    private static final Logger log = LoggerFactory.getLogger(NullRadioMedium.class);

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "Null radio medium - just forwards incoming packets to all other nodes";
    }

    @Override
    public void transmit(RadioPacket packet) {
        Simulator simulator = this.simulator;
        if (simulator == null) {
            log.error("No simulator");
            return;
        }

        // Send packet to all nodes
        Node[] nodes = simulator.getNodes();
        double rssi = packet.getTransmitPower();
        int channel = packet.getWirelessChannel();
        Node source = packet.getSource();
        if (nodes != null) {
            for (Node node : nodes) {
                if (node != source) {
                    Transciever radio = node.getRadio();
                    /* Just send if they are on same channel - no loss - no collisions - infinite range */
                    if(radio.isEnabled() && radio.getWirelessChannel() == channel) {
                        simulator.generateTransmissionEvents(packet, node, rssi);
                    }
                }
            }
        }
    }

}
