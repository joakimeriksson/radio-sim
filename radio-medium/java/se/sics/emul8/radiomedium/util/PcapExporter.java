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
package se.sics.emul8.radiomedium.util;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

public class PcapExporter {

    DataOutputStream out;

    public PcapExporter() {
    }

    public void openPcap(File pcapFile) throws IOException {
        if (out != null) {
            closePcap();
        }
        if (pcapFile == null) {
            /* pcap file not specified, use default file name */
            pcapFile = new File("radiolog-" + System.currentTimeMillis() + ".pcap");
        }
        out = new DataOutputStream(new FileOutputStream(pcapFile));
        /* pcap header */
        out.writeInt(0xa1b2c3d4);
        out.writeShort(0x0002);
        out.writeShort(0x0004);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(4096);
        out.writeInt(195); /* 195 for LINKTYPE_IEEE802_15_4 */

        out.flush();
    }

    public void closePcap() throws IOException {
        out.close();
        out = null;
    }

    /* time in microseconds */
    public void exportPacketData(long time, byte[] data) throws IOException {
        if (out == null) {
            /* pcap file never set, open default */
            openPcap(null);
        }
        try {
            /* pcap packet header */
            out.writeInt((int) (time / 1000000));
            out.writeInt((int) (time % 1000000));
            out.writeInt(data.length);
            out.writeInt(data.length);
            /* and the data */
            out.write(data);
            out.flush();
        } catch (Exception e) {
            /* ... */
        }
    }
}
