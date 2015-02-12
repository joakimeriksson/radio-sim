package se.sics.emul8.radiomedium.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eclipsesource.json.JsonObject;
import se.sics.emul8.radiomedium.util.ArrayUtils;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private int port;
    private boolean isRunning;
    private ClientConnection[] clients;

    public Server(int port) {
        this.port = port;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
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
                        } catch (Exception e) {
                            log.error("Failed to setup client from {}", clientHost, e);
                            socket.close();
                        }
                    }
                } catch (IOException e) {
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

    boolean handleMessage(ClientConnection client, JsonObject json) {
        log.info("from client {}: {}", client.getName(), json.toString());
        return true;
    }

    void clientClosed(ClientConnection client) {
        removeClient(client);
        log.debug("client from {} disconnected", client.getName());
    }
}
