package se.sics.emul8.radiomedium.net;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.emul8.radiomedium.util.ArrayUtils;
import com.eclipsesource.json.JsonObject;

public class Server implements ClientHandler {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

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
                            addClient(client);
                            client.start();
                            client.send(welcome);
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

    private int seqno;

    @Override
    public boolean handleMessage(ClientConnection client, JsonObject json) {
        log.info("from client {}: {}", client.getName(), json.toString());

        JsonObject reply = new JsonObject();
        reply.set("pong", ++seqno);
        try {
            client.send(reply);
        } catch (IOException e) {
            log.error("failed to reply to client", e);
        }

        return true;
    }

    @Override
    public void clientClosed(ClientConnection client) {
        removeClient(client);
        log.debug("client from {} disconnected", client.getName());
    }
}
