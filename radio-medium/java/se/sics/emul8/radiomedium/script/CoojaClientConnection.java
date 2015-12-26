package se.sics.emul8.radiomedium.script;
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
    
    public CoojaClientConnection(CoojaScriptEngine engine, Simulator sim) {
        coojaScriptEngine = engine;
        this.sim = sim;
    }
    
    public void startTick() {
        if (tickThread != null) {
            return;
        }
        tickThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop) {
                    System.out.println("Time: " + time);
                    sim.stepTime(time, ++myTimeId);
                    synchronized(CoojaClientConnection.this) {
                        try {
                            CoojaClientConnection.this.wait();
                            time += 1000; /* one millisecond */
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            stop = true;
                        }
                    }
                }
            }
        });
        System.out.println("Starting Tick thread.");
        tickThread.start();
    }
    
    public void stopTick() {
        stop = true;
        tickThread = null;
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
        /* This will controll elapse of time - so this will be called when last time-step is done */
        if(timeId == myTimeId) {
            synchronized(this) {
                System.out.println("*** Notify tick done!");
                this.notify();
            }
        }
    }

    @Override
    public void sendPacket(RadioPacket packet, Node destination, long id,
            double rssi) {
    }


    @Override
    public void sendEvent(SimulationEvent event) {
        if(event.getType() == SimulationEvent.EventType.LOG_MESSAGE) {
            Node mote = event.getSource();
            coojaScriptEngine.handleNewMoteOutput(
                    mote,
                    mote.getId(),
                    event.getTime(),
                    (String) event.getData("logMessage")
                    );
        }
    }

    @Override
    public void emulateToTime(long time, long timeId) {
    }
}
