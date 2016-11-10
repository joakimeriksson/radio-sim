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

import se.sics.emul8.radiomedium.net.Server;
import se.sics.emul8.radiomedium.util.PcapListener;
import se.sics.emul8.web.WebServer;

public class Main {

    private static void usage(int exit) {
        System.out.println("Usage: radiomedium [-ws] [-pcap]");
        System.exit(exit);
    }

    public static void main(String[] args) throws InterruptedException {
        PcapListener pcapListener = null;
        // web server on - or - off
        boolean web = false;
        if (System.getProperty("logback.configurationFile") == null) {
            System.setProperty("logback.configurationFile", "logback.xml");
        }

        for (int i = 0; i < args.length; i++) {
            if ("-ws".equals(args[i])) {
                web = true;
            } else if ("-pcap".equals(args[i])) {
                pcapListener = new PcapListener();
            } else {
                System.err.println("Unhandled argument: " + args[i]);
                usage(1);
            }
        }

        Simulator simulator = new Simulator();
        RadioMedium radioMedium = new NullRadioMedium();
        if (pcapListener != null) {
            simulator.addRadioListener(pcapListener);
        }
        radioMedium.setSimulator(simulator);
        simulator.setRadioMedium(radioMedium);
        Server server = new Server(Simulator.DEFAULT_PORT);
        server.setSimulator(simulator);
        server.start();

        /* Quick hack to get a small web server running - for providing simulation info */
        if (web) {
            WebServer ws = new WebServer();
            ws.setSimulator(simulator);
            try {
                ws.startWS();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (;;) {
            Thread.sleep(1000);
        }
    }

}
