package se.sics.sim.core;

import java.util.ArrayList;
import java.util.Hashtable;
import se.sics.sim.interfaces.StateListener;
import se.sics.sim.net.CC2420Node;
import se.sics.sim.policy.DummyStateListener;
import se.sics.sim.rl.EpisodeManager;
import se.sics.sim.util.Visualizer;
import com.botbox.scheduler.EventQueue;
import com.botbox.scheduler.TimeEvent;

/**
 * Describe class Simulator here.
 * 
 * 
 * Created: Tue Apr 24 08:08:12 2007
 * 
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class Simulator implements Runnable {

    public static final boolean SHOW_NETWORK = false; // was true
    public static final boolean DEBUG = false;

    public static final int FAST_TIME = 0;
    public static final int REAL_TIME = 1;
    private static boolean status = true;

    // This is a hack!!! (public for fast access...)
    public Visualizer visualizer;

    private ArrayList<Node> nodes = new ArrayList<Node>();
    private EventQueue queue = new EventQueue();
    // private ALEventQueue queue = new ALEventQueue();
    // time in microseconds.
    private long currentTime;
    private boolean running = false;
    private boolean stopped = false;
    private Setup setup;

    private long startTime = 0;
    /* 1000 is the divisor when running microsecond resolution */
    private double divisor = 1000.0;
    private int mode = FAST_TIME;
    private long count = 0;
    private static long maxPenalty = -(Integer.MAX_VALUE / 100);
    private static Hashtable<Object,Object> properties;
    private static Setup simulationSetup;
    private static int numberOfNodes = -1;
    private static int debug = 0;

    /**
     * Creates a new <code>Simulator</code> instance.
     * 
     */
    public Simulator() {
    }

    public void start() {
        if (!running) {
            running = true;
            new Thread(this).start();
        }
    }

    public void run() {
        currentTime = 0;
        startTime = System.currentTimeMillis();
        init();
        TimeEvent te;
        while ((te = queue.popFirst()) != null) {
            currentTime = te.getTime();
            if (mode == REAL_TIME) {
                double realTime = (System.currentTimeMillis() - startTime) / divisor;
                if (currentTime > realTime) {
                    try {
                        // Sleep a while before execution...
                        int sleep = (int) (divisor * (currentTime - realTime));
                        Thread.sleep(sleep);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            count++;
            if (DEBUG) {
                if (count < 2000) {
                    System.out.println("Executing event no " + (count) + " at time " + currentTime + " " + te + " "
                            + te.getClass().getSimpleName());
                }
            }
            te.execute(currentTime);
        }
        status = finish();
        if (0 < debug) {
            System.out.println("Setting status to " + status);
        }
        if (visualizer != null) {
            visualizer.close();
        }
    }

    // Sets the divisor
    // if set to 1 then each simulation time unit represents one microsecond
    // if set to 0.1 then each simulation time unit represents 0.1 microseconds
    // if set to 10 then each simulation time unit represents 10 micros.
    // default is 1000.0 for real-time
    public void setSpeed(double microsPerSTU) {
        divisor = microsPerSTU;
    }

    public void setSpeedMode(int mode) {
        this.mode = mode;
    }

    public void setVisualizer(Visualizer vis) {
        visualizer = vis;
    }

    public Node[] getNodes() {
        return nodes.toArray(new Node[nodes.size()]);
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public void addEvent(TimeEvent event) {
        if (!stopped) {
            try {
                queue.addEvent(event);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                System.err.println("Stopping");
                status = false;
                stop();
            }
        } else {
            // System.out.println("Simulator was already STOPPED when adding: "
            // + event.getTime());
        }

    }

    // returns elapsed simulation time in microseconds
    public long getSimulationTime() {
        return currentTime;
    }

    // returns elapsed time in microseconds
    private long getElapsedRealTimeInMicroSeconds() {
        return (System.currentTimeMillis() - startTime) * 1000;
    }

    private double getElapsedRealTimeInSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000.;
    }

    private double getElapsedRealTimeInMS() {
        return (System.currentTimeMillis() - startTime);
    }

    private void init() {
        EpisodeManager manager = EpisodeManager.getDefault();
        if (manager != null) {
            System.out.println("Simulator is running with EpisodeManager: " + manager);
            manager.init(this);
        } else {
            // System.out.println("Simulator is running without EpisodeManager");
        }

        for (int i = 0, n = nodes.size(); i < n; i++) {
            nodes.get(i).init(this);
        }
    }

    private boolean finish() {

        long totalUtility = ((CC2420Node) nodes.get(0)).getUtility();
        // IF ONLY USING UTILITY OF NODE 0:
        boolean errorFree = true;
        for (int i = 0, n = nodes.size(); i < n; i++) {
            // one node that finished with error is enough to cause global
            // error:
            errorFree = errorFree && nodes.get(i).finish();
            // IF SUMMING TOTAL UTILITY:
            // totalUtility += ((CC2420Node) nodes.get(i)).getUtility();
            if (!errorFree) {
                break;
            }
            // status = (status && partialResult);
        } // end for

        if (!errorFree) {
            if (0 < debug) {
                System.out.println("Simulation finished with errors");
            }
            totalUtility = maxPenalty;
            // System.out.println("I was not error free. so total utility " +
            // totalUtility);
        }

        if (0 < debug) {
            System.out.println("------------------------------------");
            System.out.println("Number of nodes: " + nodes.size());
            System.out.println("Elapsed simulation time in microsec: " + (currentTime));
            System.out.println("Elapsed real time: " + getElapsedRealTimeInSeconds());
        }

        Double previousUtility = (Double) properties.get("policy.lastGlobalUtility");
        if (0 < debug) {
            if (previousUtility != null) {
                System.out.println("Total utility is " + totalUtility + " compared with last this is a increase of "
                        + (totalUtility - previousUtility));
            } else {
                System.out.println("Total utility is " + totalUtility);
            }
            System.out.println("Number of events processed: " + count);
        }

        // store the total utility of this round:
        properties.put("policy.lastGlobalUtility", (double) totalUtility);
        properties.put("policy.lastPolicy", nodes.get(0).stateListener.getPolicyAsText());
        // compare with previous best result
        Double bestUtility = (Double) properties.get("policy.bestGlobalUtility");
        if (bestUtility == null || (bestUtility != null && bestUtility < totalUtility)) {
            properties.put("policy.bestGlobalUtility", (double) totalUtility);
            properties.put("policy.bestPolicyAsText", nodes.get(0).stateListener.getPolicyAsText());
            properties.put("policy.bestPolicy", nodes.get(0).stateListener.getPolicy());
        } // else do nuffin

        setup.finish(this);
        nodes.get(0).stateListener.finish(this);

        // EpisodeManager manager = EpisodeManager.getDefault();
        // if (manager != null) {
        // manager.finish(this);
        // }
        return errorFree;
    }

    public void setup(Setup s) {
        setup = s;
        s.setup(this);
    }

    public static void main(String[] args) {
        ArrayList<String> properties = null;
        String cls = "LineSetup";
        String defaultNodeType = "RBPNode";
        String protocolType = ""; // XMAC.class.getSimpleName();
        String applicationType = ""; // BulkSend.class.getSimpleName();
        String policyType = DummyStateListener.class.getSimpleName();
        Node.resetGlobalIDCounter();
        status = true;

        boolean showNetwork = SHOW_NETWORK;
        int speed = 100;
        boolean realtime = false;
        String[] nodeTypes = null;

        {
            int index = 0, l, n = args.length;
            for (; index < n; index++) {
                if (args[index].equals("-gui")) {
                    showNetwork = true;
                } else if (args[index].equals("-nogui")) {
                    showNetwork = false;
                } else if (args[index].startsWith("-h")) {
                    System.out.println("Usage: " + Simulator.class.getName()
                            + " [-gui] [-nogui] SetupClass [NodeClass1 ...]");
                    System.exit(0);
                } else if ((l = args[index].indexOf('=')) > 0) {
                    String pname = args[index].substring(0, l);
                    String pvalue = args[index].substring(l + 1);
                    if (pname.equals("speed")) {
                        speed = Integer.parseInt(pvalue);
                    } else if (pname.equals("realtime")) {
                        realtime = Boolean.parseBoolean(pvalue);

                    } else if (pname.equals("gui")) {
                        showNetwork = Boolean.parseBoolean(pvalue);

                    } else {
                        if (properties == null) {
                            properties = new ArrayList<String>();
                        }

                        properties.add(pname);
                        properties.add(pvalue);
                        if (0 < debug) {
                            System.out.println("PROPERTY: " + pname + " = " + pvalue);
                        }
                        // cheating...
                        if (pname.equals("network.numberOfNodes")) {
                            numberOfNodes = Integer.parseInt(pvalue);
                        } else if (pname.equals("simulation.resetFlag")) {
                            if (pvalue.equals("true")) {
                                System.out.println("DO RESET");
                                simulationSetup = null;
                            }
                        } else if (pname.equals("simulation.nodeType")) {
                            nodeTypes = new String[] { pvalue };
                        } else if (pname.equals("simulation.protocolType")) {
                            protocolType = pvalue;
                        } else if (pname.equals("simulation.applicationType")) {
                            applicationType = pvalue;
                            if (0 < debug) {
                                System.out.println("Setting applicationType to " + applicationType);
                            }
                        } else if (pname.equals("simulation.policyType")) {
                            policyType = pvalue;
                            if (0 < debug) {
                                System.out.println("Setting policyType to " + policyType);
                            }
                        } else if (pname.equals("simulation.maxPenalty")) {
                            maxPenalty = Long.parseLong(pvalue);
                        }

                    }
                } else {
                    // break;
                }
            }
            // if (index < n) {
            // cls = args[index++];
            // }
            if (nodeTypes == null) {
                nodeTypes = new String[] { defaultNodeType };
            }

            // if (protocolType == null) {
            // System.out.println("Setting protocolType to " + protocolType);
            //
            // }

        }

        try {
            Simulator sim = new Simulator();
            if (simulationSetup == null) {
                // first run!
                System.out.println("Simulator is creating a simulation setup for the first run");
                simulationSetup = (Setup) Class.forName("se.sics.sim.examples." + cls).newInstance();

                // Make sure all classes can be found
                Class<?>[] nodeClasses = new Class<?>[nodeTypes.length];
                for (int i = 0, n = nodeTypes.length; i < n; i++) {
                    nodeClasses[i] = Class.forName("se.sics.sim.net." + nodeTypes[i]);
                }
                Class<?> macLayerClass = null;
                if (0 < protocolType.length()) {
                    macLayerClass = Class.forName("se.sics.sim.protocols." + protocolType);
                }
                Class<?> applicationClass = null;
                if (0 < applicationType.length()) {
                    applicationClass = Class.forName("se.sics.sim.applications." + applicationType);
                }
                Class<?> policyClass = null;
                if (0 < policyType.length()) {
                    policyClass = Class.forName("se.sics.sim.policy." + policyType);
                }

                // for (int i = 0, n = nodeTypes.length; i < n; i++) {
                System.out.println();
                System.out.println("---- Simulation (" + cls + "=>" + nodeClasses[0].getName() + ") -----");

                simulationSetup.setProperty("node.class", nodeClasses[0]);
                if (macLayerClass != null) {
                    simulationSetup.setProperty("macLayer.class", macLayerClass);
                }
                if (applicationClass != null) {
                    simulationSetup.setProperty("application.class", applicationClass);
                }
                if (policyClass != null) {
                    StateListener policyInstance = null;
                    try {
                        policyInstance = (StateListener) policyClass.newInstance();
                        policyInstance.init(sim);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    simulationSetup.setProperty("policy.instance", policyInstance);
                }

                // if (SHOW_NETWORK) { //showNetwork) {
                // try {
                // sim.setVisualizer(new WSNManConnection(14400));
                // } catch (Exception e) {
                // e.printStackTrace();
                // }
                // }
            } else {
                // System.out
                // .println("Simulator is reusing existing simulation setup");

                StateListener policyInstance = (StateListener) simulationSetup.getProperty("policy.instance");
                policyInstance.reinit(sim);
                simulationSetup.setProperty("policy.instance", policyInstance);

            }

            // in any case, make sure we have access to current properties
            if (properties != null) {
                for (int j = 0, m = properties.size(); j < m; j += 2) {
                    simulationSetup.setProperty(properties.get(j), properties.get(j + 1));
                }
            }
            sim.setProperties(simulationSetup.getProperties());

            sim.setSpeedMode(realtime ? REAL_TIME : FAST_TIME);
            sim.setSpeed(speed);
            sim.setup(simulationSetup);
            // System.out.println("Before running simulation");
            sim.run();
            // System.out.println("After running simulation");
            // System.out.println();
            // } //end un-needed for-loop
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setProperties(Hashtable<Object,Object> properties) {
        this.properties = properties;

    }

    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public void stop() {

        // try {
        // int er = 1 / 0;
        // } catch(Exception e) {
        // e.printStackTrace();
        // }
        running = false;
        stopped = true;
        queue = new EventQueue();
    }

    public boolean getStatus() {
        return status;
    }

    public Object getResult(int resultType) {
        // could do something fancy with introspection, but noo
        return properties.get("policy.lastPolicy");
    }
}
