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
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eclipsesource.json.JsonObject;

import se.sics.emul8.radiomedium.Node;
import se.sics.emul8.radiomedium.RadioPacket;
import se.sics.emul8.radiomedium.SimulationEvent;

public class JSONClientConnection extends ClientConnection {

    private static final Logger log = LoggerFactory.getLogger(JSONClientConnection.class);
    private static final byte[] NEW_LINE = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_PAYLOAD_SIZE = 20 * 1024 * 1024;

    private static final int TIMEOUT = 10000;
    
    private boolean useLength = true; /* Set to false if no line with len and attributes is to be sent */

    private final ClientHandler clientHandler;
    private Socket socket;
    private OutputStream out;
    private BufferedInputStream in;
    private String name;
    private boolean isConnected;
    private boolean isWaitingForProtocolHeader;
    private boolean hasStarted;
    private Hashtable<String, Object> clientProperties = new Hashtable<String, Object>();
    private long emulationTime; /* if this is an emulator this will be updated to reflect how far this emulator reached */

    public JSONClientConnection(ClientHandler clientHandler, Socket socket) throws IOException {
        this.clientHandler = clientHandler;
        this.socket = socket;
        this.name = "[" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ']';

        this.out = socket.getOutputStream();
        this.in = new BufferedInputStream(socket.getInputStream());
        this.isConnected = true;
        log.debug("{} client connected", this.name);
    }

    public JSONClientConnection(ClientHandler clientHandler, String host, int port) throws IOException {
        InetAddress addr = InetAddress.getByName(host);
        InetSocketAddress sockaddr = new InetSocketAddress(addr, port);

        log.info("Connecting to {}:{}...", host, port);

        this.socket = new Socket();
        this.socket.connect(sockaddr, TIMEOUT);

        this.clientHandler = clientHandler;
        this.name = "[" + this.socket.getInetAddress().getHostAddress() + ":" + this.socket.getPort() + ']';

        this.out = this.socket.getOutputStream();
        this.in = new BufferedInputStream(this.socket.getInputStream());

        // Connecting to server that should respond with a protocol header
        this.isWaitingForProtocolHeader = true;

        this.isConnected = true;
        log.debug("{} client connected", this.name);
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

    public boolean isConnected() {
        return isConnected;
    }

    public void sendRawData(byte[] data) throws IOException {
        OutputStream output = this.out;
        if (output != null) {
            output.write(data);
            output.flush();
        }
    }

    public void start() {
        if (this.hasStarted) {
            return;
        }
        this.hasStarted = true;

        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    processInput(in);
                } catch (Exception e) {
                    if (isConnected) {
                        log.error("{} connection closed", getName(), e);
                    }
                } finally {
                    close();
                }
            }
        }, name);
        t.start();
    }

    protected void processInput(BufferedInputStream input) throws IOException {
        boolean isParsingJSON = false;
        boolean stuffed = false;
        boolean quoted = false;
        int brackets = 0;

        if (input == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        while (isConnected()) {
            int c = input.read();
            if (c < 0) {
                close();
                break;
            }
            if (c == '{' && !isParsingJSON) {
                isParsingJSON = true;
            }
            if (c == '\r') {
                // Ignore CR
                continue;
            }

            if (isParsingJSON) {
                sb.append((char) c);

                if (stuffed) {
                    stuffed = false;
                } else if (c == '\\') {
                    stuffed = true;
                } else if (quoted) {
                    if (c == '"') {
                        quoted = false;
                    }
                } else if (c == '"') {
                    quoted = true;
                } else if (c == '{') {
                    brackets++;
                } else if (c == '}') {
                    brackets--;
                    if (brackets == 0) {
                        // log.debug("Read JSON: " + sb);
                        JsonObject json = JsonObject.readFrom(sb.toString());
                        sb.setLength(0);
                        isParsingJSON = false;
                        log.debug("JSON without len recived.");
                        if (!clientHandler.handleMessage(this, json)) {
                            // This connection should no longer be kept alive
                            break;
                        }
                    }
                }
                continue;
            }

            if (c == '\n') {
                // End of line
                String parameters = sb.toString();
                sb.setLength(0);
                if(parameters.trim().length() == 0) {
                    continue;
                }

                if (isWaitingForProtocolHeader) {
                    // TODO Verify protocol header.
                    log.debug("server protocol version: " + parameters);
                    if (!parameters.startsWith("RSIM ")) {
                        throw new IOException("unsupported protocol: " + parameters);
                    }
                    isWaitingForProtocolHeader = false;
                    continue;
                }

                String[] attrs = parameters.split(";");
                int dataSize = Integer.parseInt(attrs[0]);
                if (dataSize > MAX_PAYLOAD_SIZE) {
                    throw new IOException("too large payload: " + dataSize);
                }

                if (dataSize == 0) {
                    // No data, only parameters
                    continue;
                }

                if (dataSize < 0) {
                    // No size specified. Assume JSON and read until end of JSON.
                    // log.debug("No message size - assume JSON and read until end of JSON data");
                    isParsingJSON = true;
                    stuffed = false;
                    quoted = false;
                    brackets = 0;
                    continue;
                }

                byte[] data = new byte[dataSize];

                // Read all data
                for (int i = 0, n = 0; i < dataSize; i += n) {
                    n = in.read(data, i, dataSize - i);
                    if (n < 0) {
                        throw new EOFException();
                    }
                }

                // Assume type JSON for now
                String jsonData = new String(data, StandardCharsets.UTF_8);
                JsonObject json = JsonObject.readFrom(jsonData);
                if (!clientHandler.handleMessage(this, json)) {
                    // This connection should no longer be kept alive
                    break;
                }
            } else {
                sb.append((char)c);
            }
        }
    }

    public boolean send(JsonObject json) throws IOException {
        return send(json, null);
    }

    public boolean send(JsonObject json, Map<String,String> attributes) throws IOException {
        OutputStream output = this.out;
        if (output != null) {
            byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);
            if(useLength) {
                output.write(Integer.toString(data.length).getBytes(StandardCharsets.US_ASCII));
                if (attributes != null && attributes.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (String key : attributes.keySet()) {
                        String value = attributes.get(key);
                        sb.append(';').append(key).append('=').append(value);
                    }
                    output.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
                }
                output.write(NEW_LINE);
                output.write(data);
                output.flush();
            } else {
                output.write(data);
                output.write(NEW_LINE);
            }
            System.out.println("Data:'" + new String(data) + "'" + " len:" + data.length);
            return true;
        }
        return false;
    }

    public void close() {
        boolean isDisconnecting = isConnected;
        isConnected = false;
        try {
            if (isDisconnecting) {
                log.debug("{} disconnecting", getName());
                clientHandler.clientClosed(this);
            }

            if (out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (socket != null) {
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e) {
                    if (isDisconnecting) {
                        log.error("{} failed to close connection", getName(), e);
                    }
                }
            }
            if (isDisconnecting) {
                log.debug("{} disconnected", getName());
            }
        } catch (Exception e) {
            log.error("{} failed to disconnect", getName(), e);
        }
    }

    /* tell emulators to run until a specific time */
    public void emulateToTime(long time, long timeId) {
        JsonObject json = new JsonObject();
        json.add("command", "time-set");
        json.add("id", timeId);
        json.add("params", new JsonObject().add("time", time));
        
        try {
            send(json);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public boolean setTime(long time, long timeId) {
        if (emulationTime < time) {
            emulationTime = time;
            return true;
        }
        return false;
    }

    /* called when the time in the simulator has stepped to the desired time */
    public void timeStepDone(long timeId) {
        try {
            send(new JsonObject().add("reply", "OK").add("id", timeId));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendPacket(RadioPacket packet, Node destination, long id, double rssi) {
        JsonObject json = packet.toJsonDestination(destination, rssi);
        json.add("id", id);
        try {
            send(json);
        } catch (IOException e) {
            log.error("failed to deliver radio packet to node {}", destination.getId(), e);
        }
    }

    public boolean sendLogMsg(int nodeId, String string) {
        JsonObject json = new JsonObject();
        json.add("command", "log");
        json.add("params", new JsonObject().add("node-id", nodeId).add("message", string));
        log.debug("Sending log msg:" + string);
        try {
            send(json);
            return true; /* success */
        } catch (IOException e) {
            log.error("failed to log message from node {}", nodeId, e);
        }
        return false; /* failed */
    }

    public void sendEvent(SimulationEvent event) {
        JsonObject eventData = null;
        String type = null;
        String source = "";
        switch(event.getType()) {
        case LOG_MESSAGE:
            type = "log";
            source = event.getSource().getId();
            eventData = new JsonObject();
            eventData.add("logMessage", (String) event.getData("logMessage"));
            break;
        default:
            /* ignore others for now */
            break;
        }
        if (eventData != null) {
            JsonObject json = new JsonObject();
            JsonObject eventObject = new JsonObject();
            eventObject.add("time", event.getTime());
            eventObject.add("type", type);
            eventObject.add("source", source);
            eventObject.add("event-data", eventData);
            json.add("event", eventObject);
            json.add("id", 0);
            try {
                send(json);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
