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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.emul8.radiomedium.Simulator;
import se.sics.emul8.radiomedium.util.ArrayUtils;
import com.eclipsesource.json.JsonObject;

public class Server implements ClientHandler {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private static final byte[] PROTOCOL_HEADER = "RSIM 1.0\r\n".getBytes(StandardCharsets.US_ASCII);

    private Simulator simulator;
    private SimulatorJSONHandler simulatorJSONHandler;
    private int port;
    private boolean hasStarted;
    private boolean isRunning;
    private JsonObject welcome;
    private ClientConnection[] clients;

    public Server(int port) {
        this.port = port;

        JsonObject rm = new JsonObject();
        rm.set("name", "Cooja radio medium");
        rm.set("api-version", "0.0.1");

        welcome = new JsonObject();
        welcome.set("radio-medium", rm);
        welcome.set("status", "OK");
    }

    public JsonObject getWelcomeMessage() {
        return welcome;
    }

    public void setWelcomeMessage(JsonObject json) {
        this.welcome = json;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        if (this.hasStarted) {
            return;
        }
        this.hasStarted = true;
        this.isRunning = true;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    Socket socket = null;
                    log.info("Server started. Waiting for client connections at port {}.", port);
                    while(isRunning) {
                        try {
                            socket = serverSocket.accept();
                        } catch (IOException e) {
                            log.error("Failed to accept client connection", e);
                            System.exit(-1);
                        }

                        String clientHost = socket.getInetAddress().getHostAddress();
                        log.debug("New client from {}", clientHost);
                        try {
                            ClientConnection client = new ClientConnection(Server.this, socket);
                            client.sendRawData(PROTOCOL_HEADER);
                            addClient(client);
                            client.start();
                            if (welcome != null) {
                                client.send(welcome);
                            }
                        } catch (Exception e) {
                            log.error("Failed to setup client from {}", clientHost, e);
                            socket.close();
                        }
                    }
                } catch (Exception e) {
                    log.error("Server listen on port {} failed", port, e);
                } finally {
                    log.info("Server stopped");
                    System.exit(-1);
                }

            }
        }, "server");

        t.start();
    }

    private synchronized void addClient(ClientConnection client) {
        clients = ArrayUtils.add(ClientConnection.class, clients, client);
    }

    private synchronized void removeClient(ClientConnection client) {
        clients = ArrayUtils.remove(clients, client);
    }

    public void close() {
        isRunning = false;
    }

    @Override
    public boolean handleMessage(ClientConnection client, JsonObject json) {
        log.info("from client {}: {}", client.getName(), json.toString());
        if (simulator == null) {
            log.error("simulation not set...");
            return false;
        }
        return simulatorJSONHandler.handleMessage(client, json);
    }

    @Override
    public void clientClosed(ClientConnection client) {
        removeClient(client);
        log.debug("client from {} disconnected", client.getName());
    }

    public void setSimulator(Simulator simulator) {
        this.simulator = simulator;
        simulatorJSONHandler = new SimulatorJSONHandler(simulator);
    }

    public ClientConnection[] getClients() {
        // TODO Auto-generated method stub
        return clients;
    }
}