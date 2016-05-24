package se.sics.emul8.mspsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import se.sics.emul8.radiomedium.Position;
import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.emul8.radiomedium.net.ClientHandler;
import se.sics.emul8.radiomedium.net.JSONClientConnection;
import se.sics.emul8.radiomedium.util.ArrayUtils;
import se.sics.mspsim.core.Memory.AccessType;
import se.sics.mspsim.core.MemoryMonitor;
import se.sics.mspsim.chip.DS2411;
import se.sics.mspsim.chip.Radio802154;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.Memory;
import se.sics.mspsim.core.OperatingModeListener;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.MapEntry;
import se.sics.mspsim.util.MapTable;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
/*
 * Emulation tool for MSPSim based emulations that connect to radio-sim's radio medium.
 * 
 * java -jar ... -yaml => reads a yaml from standard in
 * 
 * YAML format:
 * A map of key-value's where
 * nodes: is a list of nodes that define the nodes ID and position in the simulation.
 * 
 * Example:
 * nodes:
 * - {id: 1, pos: [1,2]}
 * - {id: 2, pos: [12,4]}
 * - ...
 * other config....
 * 
 * The format for position is [x,y]
 * 
 */

public class EmuLink implements ClientHandler {

    private static final Logger log = LoggerFactory.getLogger(EmuLink.class);

    private static final int DEFAULT_PORT = 7711;
    private static final long DURATION = 1000;

    private final JSONClientConnection clientConnection;
    private NodeHandler[] nodes;
    private HashMap<String,NodeHandler> nodeMap = new HashMap<String, NodeHandler>();
    
    private final boolean isTimeController;
    private long myTime;
    private AtomicLong messageId = new AtomicLong();
    private long timeId;
    private volatile boolean isWaitingForTimeReply;
    private long controllerTime;
    
    public EmuLink(String host, int port, boolean isTimeController) throws IOException {
        this.clientConnection = new JSONClientConnection(this, host, port);
        this.isTimeController = isTimeController;
    }

    private void addNode(NodeHandler node, int nodeId) {
        // TODO Auto-generated method stub
        nodes = ArrayUtils.add(NodeHandler.class, nodes, node);
        nodeMap.put("" + nodeId, node);
    }
    
    public ClientConnection getConnection() {
        return this.clientConnection;
    }

    public void connect() {
        this.clientConnection.start();
    }
    
    private JsonObject createCommand(String cmd, JsonObject params) {
        JsonObject jsonCmd = new JsonObject().add("command",  cmd).add("parameters", params);
        return jsonCmd;
    }

    private void sendInit(String nodeIdStr) throws IOException {
        log.debug("Registering node with ID: " + nodeIdStr);
        JsonObject config = new JsonObject();
        int nodeid = Integer.parseInt(nodeIdStr);
        NodeHandler node = nodeMap.get(nodeIdStr);
        config.add("node-id", nodeid);
        Position pos = node.getPosition();
        if (pos != null) {
            JsonArray jpos = new JsonArray();
            jpos.add(pos.x).add(pos.y).add(pos.z);
            config.add("position", jpos);
        }
        JsonObject reqNode = createCommand("node-config-set", config);
        clientConnection.send(reqNode);
    }

    void sendLog(int nodeId, String log) throws IOException {
        JsonObject reqNode = createCommand("log", new JsonObject().add("node-id", nodeId).add("message", log));
        clientConnection.send(reqNode);
    }

    char hex[] = "0123456789ABCDEF".toCharArray();

    private String hex(int i) {
        String s;
        i = i & 0xff;
        s = "" + hex[(i / 16)] + hex[i & 0x0f];
        return s;
    }
    
    void sendPacket(int nodeId, byte[] packet) throws IOException {
        StringBuilder sb = new StringBuilder();
        JsonObject transmit = new JsonObject();
        transmit.add("command", "transmit");
        transmit.add("node-id", nodeId);
        for(int i = 0; i < packet.length; i++) {
            sb.append(hex(packet[i]));
        }
        transmit.add("packet-data", sb.toString());
        transmit.set("time", myTime);
        clientConnection.send(transmit);
    }

    private void serveForever() {
        JsonObject timeMessage = new JsonObject();
        JsonObject params = new JsonObject();
        timeMessage.add("command", "time-set");
        timeMessage.add("parameters", params);
        connect();
        /* register all the nodes */
        try {
            for (String node : nodeMap.keySet()) {
                sendInit(node);
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        while (true) {
            try {
                if (isTimeController) {
                    if (isWaitingForTimeReply) {
                        Thread.sleep(10);
                    } else {
                        timeId = messageId.incrementAndGet();
                        controllerTime += DURATION;
                        params.set("time", controllerTime);
                        timeMessage.set("id", timeId);
                        isWaitingForTimeReply = true;
                        clientConnection.send(timeMessage);
                        Thread.sleep(5);
                    }
                } else {
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean handleMessage(ClientConnection clientConnection, JsonObject json) {
        System.out.println("RECV: " + json);
        String cmd = json.getString("command", null);
        String replyStr = json.getString("reply", null);
        JsonObject reply = new JsonObject();
        long id;
        if ((id = json.getLong("id", -1)) != -1) {
            reply.set("id", id);
        }
        if (replyStr != null) {
            if (replyStr.equals("OK") && id == timeId) {
                // OK on time message
                isWaitingForTimeReply = false;
            } else if (id >= 0) {
                log.error("Unexpected reply:" + json.toString());
            }

        } else if (cmd != null) {
            if (cmd.equals("time-step")) {
                JsonObject params = json.get("parameters").asObject();
                long newTime = params.getLong("time", 0);
//                System.out.println("Accepting time elapsed." + (newTime - myTime));
                if (newTime > myTime) {
                    for (int i = 0; i < nodes.length; i++) {
                        GenericNode node = nodes[i].getNode();
                        node.getCPU().stepMicros(0, newTime);
                    }
                    myTime = newTime;
                }
                reply.set("reply", "OK");
                try {
                    ((JSONClientConnection)clientConnection).send(reply);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (cmd.equals("transmit") || cmd.equals("receive")) {
                String destId = json.getString("node-id", null);
                /* all packets should go into the radio? */
                String data = json.getString("packet-data", null);
                System.out.println("Transmission for node: " + destId + " " + data);
                NodeHandler node = nodeMap.get(destId);
                if (node != null) {
                    node.packetReceived(data);
                } else {
                    log.debug("No Radio for this id!");
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
        if (System.getProperty("logback.configurationFile") == null) {
            System.setProperty("logback.configurationFile", "logback.xml");
        }

        ArgumentManager config = new ArgumentManager();
        config.handleArguments(args);
        config.setProperty("cli", "false");

        int nodeId = config.getPropertyAsInt("id", -1);
        boolean isTimeController = config.getPropertyAsBoolean("timectrl", false);
        EmuLink c = new EmuLink("127.0.0.1", DEFAULT_PORT, isTimeController);
        boolean nodesCreated = false;
        
        /* If the yaml argument is given as a boolean we will just read std. input until a empty line */
        if(config.getPropertyAsBoolean("yaml", false)) {
            Yaml yaml = new Yaml();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            StringBuilder buf = new StringBuilder();
            String line = "";
            /* this will only allow non-empty lines... */
            while((line = reader.readLine()) != null) {
                if(line.equals("")) {
                    // Break at first empty line?
                    break;
                } else {
                    buf.append(line).append('\n');
                }
            }
            reader.close();
            System.out.println("CNF:" + buf.toString());
            Object conf = yaml.load(buf.toString());
            System.out.println("CONFIG:" +  conf);
            
            if (conf instanceof Map) {
                @SuppressWarnings("rawtypes")
                Map map = (Map) conf;
                Object val = map.get("nodes");
                /* found the node-list */
                if (val instanceof List) {
                    for (Map<String,Object> nodeMap : (List<Map>) val) {
                        System.out.println("Found node:" + nodeMap.get("id") + " pos:" + nodeMap.get("position"));
                        int id = getInt(nodeMap.get("id"));
                        NodeHandler node = setupNode(id, config.getProperty("nodeType"), config.getProperty("platform"), config, c);
                        @SuppressWarnings("rawtypes")
                        List pos = (List) nodeMap.get("position");
                        if (pos != null) {
                            double x = 0, y = 0, z = 0;
                            x = getDouble(pos.get(0));
                            y = getDouble(pos.get(1));
                            if (pos.size() > 2) {
                                z = getDouble(pos.get(2));
                            }
                            node.getPosition().set(x, y, z);
                        }
                        c.addNode(node, id);
                        nodesCreated = true;
                    }
                }
            }
        }

        /* if no nodes are created with other config - create one here */
        if (!nodesCreated) {
            NodeHandler node = setupNode(nodeId, config.getProperty("nodeType"), config.getProperty("platform"), config, c);
            c.addNode(node, nodeId);
        }
        c.serveForever();
    }

    private static double getDouble(Object object) {
        if(object instanceof Number) {
            return ((Number) object).doubleValue();
        }
        return Double.parseDouble("" + object);
    }

    private static int getInt(Object object) {
        if(object instanceof Number) return ((Number)object).intValue();
        return Integer.parseInt("" + object);
    }

    public static NodeHandler setupNode(int nodeId, String nodeType, String platform, ArgumentManager config, EmuLink c) throws IOException {
        GenericNode node;
        if (nodeType != null) {
            node = se.sics.mspsim.Main.createNode(nodeType);
        } else {
            if (platform == null) {
                // Default platform
                platform = "sky";

                // Guess platform based on firmware filename suffix.
                // TinyOS firmware files are often named 'main.exe'.
                String[] a = config.getArguments();
                if (a.length > 0 && !"main.exe".equals(a[0])) {
                    int index = a[0].lastIndexOf('.');
                    if (index > 0) {
                        platform = a[0].substring(index + 1);
                    }
                }
            }
            nodeType = se.sics.mspsim.Main.getNodeTypeByPlatform(platform);
            node = se.sics.mspsim.Main.createNode(nodeType);
        }
        if (node == null) {
            System.err.println("MSPSim does not currently support the platform '" + platform + "'.");
            System.exit(1);
        }
        node.setupArgs(config);

        DS2411 ds2411 = node.getCPU().getChip(DS2411.class);
        if (ds2411 != null) {
            if (nodeId == -1) {
                nodeId = ((int)(Math.random() * 255)) & 0xff;
            }
            System.out.println("Setting MAC/NodeID to " + nodeId);
            ds2411.setMACID(nodeId & 0xff, nodeId & 0xff, nodeId & 0xff, (nodeId >> 8) & 0xff, nodeId & 0xff, nodeId & 0xff);
        }

        System.out.println("**** ID = " + nodeId);

        ComponentRegistry r = node.getRegistry();

        /* Only works with 802154 radio for now */
        Radio802154 radio = node.getCPU().getChip(Radio802154.class);
        System.out.println("Radio:" + radio);
        RadioWrapper wradio = null;
        if(radio != null) {
            radio.setRSSI(-100); /* needed to set the CCA to 1 */

            System.out.println("*** Installing radio listener...");
            wradio = new RadioWrapper(radio);
            radio.addOperatingModeListener(new OperatingModeListener() {
                public void modeChanged(Chip arg0, int arg1) {
//                    System.out.println("Radio mode changed:" + arg1);
                }
            });
        }

        NodeHandler nodeHandler = new NodeHandler(nodeId, c, node, wradio);
        if(wradio != null) {
            wradio.addPacketListener(nodeHandler);
        }

        USART uart = r.getComponent(USART.class);
        if (uart != null) {
            uart.addUSARTListener(nodeHandler);
        }
        
        /* Configure node-id for one node */
        MapTable map = r.getComponent(MapTable.class);
        System.out.println("*** MAP:" + map);
        if (map != null) {
            /* Try to find Node ID for Contiki */
            MapEntry[] entries = map.getEntries("node_id");
            if (entries != null) {
                System.out.printf("Entry: %04x\n",entries[0].getAddress());
                final Memory mem = node.getCPU().getMemory();
                final int node_id = entries[0].getAddress();
                int id = mem.read(entries[0].getAddress(),Memory.AccessMode.WORD, Memory.AccessType.READ);
                System.out.printf("Value: %02x\n",id);
                node.getCPU().addWatchPoint(entries[0].getAddress(), new MemoryMonitor() {
                    @Override
                    public void notifyReadAfter(int arg0,
                            se.sics.mspsim.core.Memory.AccessMode arg1,
                            AccessType arg2) {
                    }

                    @Override
                    public void notifyReadBefore(int arg0,
                            se.sics.mspsim.core.Memory.AccessMode arg1,
                            AccessType arg2) {
                        System.out.println("Read from *** NODE_ID ***");
                        mem.set(node_id, 4711, Memory.AccessMode.WORD);
                    }

                    @Override
                    public void notifyWriteAfter(int arg0, int arg1,
                            se.sics.mspsim.core.Memory.AccessMode arg2) {
                        System.out.println("Write to *** NODE_ID ***");
                        mem.set(node_id, 4711, Memory.AccessMode.WORD);
                    }

                    @Override
                    public void notifyWriteBefore(int arg0, int arg1,
                            se.sics.mspsim.core.Memory.AccessMode arg2) {
                    }
                    
                });
            }
        }
        return nodeHandler;
    }
    
}
