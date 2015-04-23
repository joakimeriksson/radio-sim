package se.sics.emul8.radiomedium.net;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eclipsesource.json.JsonObject;

public final class ClientConnection {

    private static final Logger log = LoggerFactory.getLogger(ClientConnection.class);
    private static final byte[] NEW_LINE = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_PAYLOAD_SIZE = 20 * 1024 * 1024;

    private Server server;
    private Socket socket;
    private OutputStream out;
    private BufferedInputStream in;
    private String name;
    private boolean isConnected;
    private boolean hasStarted;

    public ClientConnection(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.name = "[" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ']';

        this.out = socket.getOutputStream();
        this.in = new BufferedInputStream(socket.getInputStream());
        this.isConnected = true;
        log.debug("{} client connected", this.name);
    }

    public String getName() {
        return name;
    }

    public boolean isConnected() {
        return isConnected;
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
        StringBuilder sb = new StringBuilder();

        if (input == null) {
            return;
        }

        while (isConnected()) {
            int c = input.read();
            if (c < 0) {
                close();
                break;
            }
            if (c == '\r') {
                // Ignore CR
                continue;
            }
            if (c == '\n') {
                // End of line
                String parameters = sb.toString();
                String[] attrs = parameters.split(";");
                int dataSize = Integer.parseInt(attrs[0]);
                if (dataSize > MAX_PAYLOAD_SIZE) {
                    throw new IOException("too large payload: " + dataSize);
                }
                byte[] data = new byte[dataSize];
                sb.setLength(0);

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
                if (!server.handleMessage(this, json)) {
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
            byte[] data = json.asString().getBytes(StandardCharsets.UTF_8);
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
                server.clientClosed(this);
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

}
