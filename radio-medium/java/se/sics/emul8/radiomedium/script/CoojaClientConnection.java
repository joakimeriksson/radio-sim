package se.sics.emul8.radiomedium.script;
import java.util.ArrayList;
import com.eclipsesource.json.JsonObject;
import se.sics.emul8.radiomedium.Node;
import se.sics.emul8.radiomedium.RadioPacket;
import se.sics.emul8.radiomedium.SimulationEvent;
import se.sics.emul8.radiomedium.Simulator;
import se.sics.emul8.radiomedium.net.ClientConnection;

public class CoojaClientConnection extends ClientConnection {

    CoojaScriptEngine coojaScriptEngine;
    Simulator sim;
    
    private long time = 0;
    private long myTimeId = 0;
    private boolean stop = false;
    private Thread tickThread;
    
    private ArrayList<SimulationEvent> events = new ArrayList<SimulationEvent>();
    private long timeout = 60 * 60 * 1000 * 1000; /* 1 hours default timeout */
    
    public CoojaClientConnection(CoojaScriptEngine engine, Simulator sim) {
        coojaScriptEngine = engine;
        this.sim = sim;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    private void handleAllEvents() {
        if(events.size() == 0) {
            return;
        }
        for (SimulationEvent event : events) {
            Node mote = event.getSource();
            System.out.println("Log MSG: " + event.getData("logMessage").toString().trim() + " " + Thread.currentThread().getName());
            coojaScriptEngine.handleNewMoteOutput(
                    mote,
                    mote.getId(),
                    event.getTime(),
                    (String) event.getData("logMessage"));
        }
        events.clear();
    }
    
    public void startTick() {
        if (tickThread != null) {
            return;
        }
        tickThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop) {
                    sim.stepTime(time, ++myTimeId);
                    System.out.print("[");
                    synchronized(CoojaClientConnection.this) {
                        try {
                            CoojaClientConnection.this.wait();
                            handleAllEvents();
                            time += 1000000; /* one millisecond */
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            stop = true;
                        }
                    }
                    if(time > timeout) {
                        /* stop simulation */
                        stop = true;
                        coojaScriptEngine.timeoutScript();
                        System.out.println("Tick Thread Stopped.");
                    }
                }
            }
        });
        tickThread.setName("CoojaTestTickThread");
        System.out.println("Starting Tick thread.");
        tickThread.start();
    }
    
    public void stopTick() {
        stop = true;
        tickThread = null;
    }
    
    public boolean isTicking() {
        return stop == false;
    }
    
    @Override
    public void close() {
    }

    @Override
    public boolean setTime(long time, long id) {
        /* called when time is set in a specific emulator */
        return true;
    }

    @Override
    public void timeStepDone(long timeId) {
        /* This will control elapse of time - so this will be called when last time-step is done */
        if(timeId == myTimeId) {
            synchronized(this) {
                System.out.print("]");
                this.notify();
            }
        } else {
            System.out.println("*** Notify tick done! - but expected:" + myTimeId + " got " + timeId);            
        }
    }

    @Override
    public void sendPacket(RadioPacket packet, Node destination, long id,
            double rssi) {
    }


    @Override
    public void sendEvent(SimulationEvent event) {
        if (event.getType() == SimulationEvent.EventType.LOG_MESSAGE) {
            events.add(event);
        }
    }

    @Override
    public void emulateToTime(long time, long timeId) {
    }

    @Override public void sendMessage(JsonObject message) {
    }
}
