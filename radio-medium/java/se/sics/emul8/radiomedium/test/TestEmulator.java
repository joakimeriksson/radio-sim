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
    private long lastTime;

    private boolean isTimeController;
    private int timeControllerSeqno = 1;
    private boolean isRunning = false;

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

    private void startTimeController() {
        if (this.clientConnection.isConnected()) {
            this.isTimeController = true;
            if (isRunning) {
                sendTimeSet();
            }
        }
    }

    private void sendTimeSet() {
        JsonObject tc = createCommand("time-set", new JsonObject().add("time", this.clientConnection.getTime() + 10));
        tc.add("id", ++this.timeControllerSeqno);
        try {
            this.clientConnection.send(tc);
        } catch (IOException e) {
            System.err.println("Failed to send 'time-set'");
            e.printStackTrace();
        }
    }

    private void serveForever() {
        JsonObject reqNode = createCommand("node-config-set", new JsonObject().add("node-id", nodeId));
        JsonObject transmit = new JsonObject();
        transmit.add("command", "transmit");
        transmit.add("node-id", nodeId);
        transmit.add("packet-data", "0102030405");
        try {
            clientConnection.send(reqNode);
            clientConnection.send(createCommand("node-config-set", new JsonObject().add("node-id", nodeId + 1)));
            clientConnection.send(createCommand("node-config-set", new JsonObject().add("node-id", nodeId + 2)));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        isRunning = true;
        if (isTimeController) {
            sendTimeSet();
        }
        while (true) {
            try {
                Thread.sleep(5000);
                if (lastTime < clientConnection.getTime()) {
                    lastTime = clientConnection.getTime();
                    transmit.set("time", lastTime);
                    clientConnection.send(transmit);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
        JSONClientConnection connection = (JSONClientConnection)clientConnection;
        String replyStatus = json.getString("reply", null);
        if (replyStatus != null) {
            return handleReply(connection, json, replyStatus);
        }
        String cmd = json.getString("command", null);
        long id = json.getLong("id", -1);
        JsonObject reply = null;
        boolean noreply = false;

        if (cmd != null) {
            if (cmd.equals("time-step")) {
                JsonObject params = json.get("parameters").asObject();
                long time = params.getLong("time", 0);
                /* Instant "emulation" */
                connection.setTime(time, id);
                System.out.println("Accepting time elapsed: " + time);
                connection.timeStepDone(id);
                noreply = true;
                //connection.sendLogMsg(nodeId, "Accepted time elapse to " + time);
            } else if (cmd.equals("transmit")) {
                String sourceId = json.getString("node-id", null);
                System.out.println("Transmission from node: " + sourceId);
                if (("" + nodeId).equals(sourceId)) {
                    connection.sendLogMsg(nodeId, "Got message from me!");
                }
            } else if (cmd.equals("receive")) {
                String destinationId = json.getString("node-id", null);
                System.out.println("Transmission for node: " + destinationId);
                if (("" + nodeId).equals(destinationId)) {
                    connection.sendLogMsg(nodeId, "Got message to me!");
                }
            }
        }
        if (reply == null && id >= 0 && !noreply) {
            // A reply is expected
            reply = createReplyObject(id);
        }

        if (reply != null) {
            try {
                /* Send a JSON reply */
                connection.send(reply);
            } catch (IOException e) {
                log.error("failed to reply to client", e);
            }
        }
        return true;
    }

    private boolean handleReply(JSONClientConnection client, JsonObject json, String replyStatus) {
        long id = json.getLong("id", -1);
        if ("OK".equals(replyStatus)) {
            if (this.isTimeController && id >= 0 && id == this.timeControllerSeqno) {
                // Time set finished. Move to next time step
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sendTimeSet();
            }
            return true;
        }
        log.error("{} error reply: {}", client.getName(), json);
        return true;
    }

    @Override
    public void clientClosed(ClientConnection clientConnection) {
        log.error("Radio medium connection closed!");
        System.exit(0);
    }

    private JsonObject createReplyObject(long id) {
        JsonObject reply = new JsonObject();
        if (id >= 0) {
            reply.set("id", id);
        }
        reply.set("reply", "OK");
        return reply;
    }

    public static void main(String[] args) throws IOException {
        TestEmulator c = new TestEmulator("127.0.0.1", 7711);
        if (args.length > 0 && args[0].equals("tc")) {
            System.out.println("Taking role as time controller");
            c.startTimeController();
        }
        c.serveForever();
    }

}
