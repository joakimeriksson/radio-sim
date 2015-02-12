package se.sics.emul8.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eclipsesource.json.JsonObject;

public class Client {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    private static final int TIMEOUT = 10000;

    private final String host;
    private final int port;
    private boolean isConnected;
    private boolean hasStarted;

    private Socket socket;
    private InputStreamReader input;
    private BufferedWriter output;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void start() throws IOException {
        if (this.hasStarted) {
            return;
        }
        this.hasStarted = true;

        InetAddress addr = InetAddress.getByName(host);
        InetSocketAddress sockaddr = new InetSocketAddress(addr, port);

        log.info("Connecting to {}:{}...", host, port);

        socket = new Socket();
        socket.connect(sockaddr, TIMEOUT);
        input = new InputStreamReader(socket.getInputStream());
        output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        isConnected = true;
        log.info("Connected to {}:{}", host, port);

        Thread t = new Thread(new Runnable() {

            @Override public void run() {
                try {
                    processInput(input);
                } catch (Exception e) {
                    if (isConnected) {
                        log.error("connection closed", e);
                    }
                } finally {
                    close();
                }
            }

        }, "client");
        t.start();
    }

    public boolean send(JsonObject json) throws IOException {
        BufferedWriter output = this.output;
        if (output != null) {
            json.writeTo(output);
            output.write("\r\n");
            output.flush();
            return true;
        }
        return false;
    }

    protected void processInput(Reader input) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[4096];
        int brackets = 0;
        boolean stuffed = false;
        boolean quoted = false;

        if (input == null) {
            return;
        }

        while (isConnected()) {
            int len = input.read(buffer, 0, buffer.length);
            if (len < 0) {
                close();
                break;
            }
            for (int i = 0; i < len; i++) {
                char c = buffer[i];
                sb.append(c);
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
                        JsonObject json = JsonObject.readFrom(sb.toString());
                        sb.setLength(0);
                        if (!handleMessage(this, json)) {
                            // This connection should no longer be kept alive
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean handleMessage(Client client, JsonObject json) {
        log.info("RECV: " + json.toString());
        return true;
    }

    public void close() {
        boolean isDisconnecting = isConnected;
        isConnected = false;
        try {
            if (isDisconnecting) {
                log.debug("disconnecting");
            }

            if (output != null) {
                output.close();
                output = null;
            }
            if (input != null) {
                input.close();
                input = null;
            }
            if (socket != null) {
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e) {
                    if (isDisconnecting) {
                        log.error("failed to close connection", e);
                    }
                }
            }
            if (isDisconnecting) {
                log.debug("disconnected");
            }
        } catch (Exception e) {
            log.error("failed to disconnect", e);
        }
    }

}
