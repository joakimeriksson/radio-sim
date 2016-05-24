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
package se.sics.emul8.radiomedium.test;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.net.ClientHandler;
import se.sics.emul8.radiomedium.net.JSONClientConnection;

import com.eclipsesource.json.JsonObject;

public class TestEmulator implements ClientHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final JSONClientConnection clientConnection;
    private int nodeId;
    private long myTime;

    public TestEmulator(String host, int port) throws IOException {
        this.clientConnection = new JSONClientConnection(this, host, port);
        this.clientConnection.start();
        nodeId = (int) (Math.random() * 10);
    }

    public ClientConnection getConnection() {
        return this.clientConnection;
    }

    private JsonObject createCommand(String cmd, JsonObject params) {
        JsonObject jsonCmd = new JsonObject().add("command", cmd).add("parameters", params);
        return jsonCmd;
    }
    
    private void serveForever() {
        JsonObject reqNode = createCommand("node-config-set", new JsonObject().add("node-id", nodeId));
        JsonObject transmit = new JsonObject();
        transmit.add("command", "transmit");
        transmit.add("node-id", nodeId);
        transmit.add("packet-data", "0102030405");
        try {
            clientConnection.send(reqNode);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        while (true) {
            try {
                Thread.sleep(5000);
                transmit.set("time", myTime);
                clientConnection.send(transmit);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean handleMessage(ClientConnection clientConnection, JsonObject json) {
        System.out.println("RECV: " + json);
        String cmd = json.getString("command", null);
        JsonObject reply = new JsonObject();
        long id = 0;
        if ((id = json.getLong("id", -1)) != -1) {
            reply.set("id", id);
        }

        if (cmd != null) {
            if (cmd.equals("time-step")) {
                JsonObject params = json.get("parameters").asObject();
                myTime = params.getLong("time", 0);
                System.out.println("Accepting time elapsed." + myTime);
                reply.set("reply", "OK");
                try {
                    ((JSONClientConnection)clientConnection).send(reply);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                ((JSONClientConnection)clientConnection).sendLogMsg(nodeId, "Accepted time elapse to " + myTime);
            } else if (cmd.equals("transmit")) {
                String destId = json.getString("destination-node-id", null);
                System.out.println("Transmission for node: " + destId);
                if (("" + nodeId).equals(destId)) {
                    ((JSONClientConnection)clientConnection).sendLogMsg(nodeId, "Got message to me!");
                }
            }
        }
        return true;
    }

    @Override
    public void clientClosed(ClientConnection clientConnection) {
        log.error("Radio medium connection closed!");
        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        TestEmulator c = new TestEmulator("127.0.0.1", 7711);
        c.serveForever();
    }

}
