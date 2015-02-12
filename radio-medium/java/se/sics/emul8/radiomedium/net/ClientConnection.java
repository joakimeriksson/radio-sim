package se.sics.emul8.radiomedium.net;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eclipsesource.json.JsonObject;

public final class ClientConnection {

    private static final Logger log = LoggerFactory.getLogger(ClientConnection.class);

    private Server server;
    private Socket socket;
    private PrintWriter out;
    private Reader in;
    private String name;
    private boolean isConnected;

    public ClientConnection(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.name = "client(" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ')';

        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new InputStreamReader(socket.getInputStream());
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
                        if (!server.handleMessage(this, json)) {
                            // This connection should no longer be kept alive
                            break;
                        }
                    }
                }
            }
        }
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
