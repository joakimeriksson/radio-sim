/*
 * Copyright (c) 2009, Swedish Institute of Computer Science.
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
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
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
 */

package se.sics.emul8.radiomedium.script;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Hashtable;
import java.util.Observer;
import java.util.concurrent.Semaphore;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.emul8.radiomedium.Node;
import se.sics.emul8.radiomedium.NullRadioMedium;
import se.sics.emul8.radiomedium.RadioMedium;
import se.sics.emul8.radiomedium.Simulator;
import se.sics.emul8.radiomedium.net.Server;

/*
import org.contikios.cooja.Cooja;
import org.contikios.cooja.Mote;
import org.contikios.cooja.SimEventCentral.LogOutputEvent;
import org.contikios.cooja.SimEventCentral.LogOutputListener;
import org.contikios.cooja.Simulation;
import org.contikios.cooja.TimeEvent;
 */

/**
 * Loads and executes a Contiki test script.
 * A Contiki test script is a Javascript that depends on a single simulation,
 * and reacts to mote log output (such as printf()s).
 *
 * @see ScriptRunner
 * @author Fredrik Osterlind (contiki / cooja version)
 * @author Joakim Eriksson (radio-sim version)
 */


public class CoojaScriptEngine {

    private static final long DEFAULT_TIMEOUT = 20*60*1000*1000; //Simulation.MILLISECOND; /* 1200s = 20 minutes */
    private static final Logger logger = LoggerFactory.getLogger(CoojaScriptEngine.class);

    private ScriptEngine engine =
            new ScriptEngineManager().getEngineByName("JavaScript");

    /* Log output listener */
    private CoojaClientConnection logOutputListener;

    private Semaphore semaphoreScript = null; /* Semaphores blocking script/simulation */
    private Semaphore semaphoreSim = null;
    private Thread scriptThread = null; /* Script thread */
    private CoojaScriptMote scriptMote;

    private boolean stopSimulation = false;

    private Simulator simulator;

    private long timeout;

    public CoojaScriptEngine(Simulator simulator) {
        this.simulator = simulator;
        logOutputListener = new CoojaClientConnection(this, simulator);
        simulator.setTimeController(logOutputListener);
    }
    
    /* Only called from the simulation loop */
    private void stepScript() {
        /* Release script - halt simulation */
        Semaphore semScript = semaphoreScript;
        Semaphore semSim = semaphoreSim;
        if (semScript == null || semSim == null) {
            return;
        }
        semScript.release();

        /* ... script executing ... */

        try {
            semSim.acquire();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        /* ... script is now again waiting for script semaphore ... */

        /* Check if test script requested us to stop */
        if (stopSimulation) {
            System.out.println("Simulation should now stop...");
        }
    }

    /* Only called from the simulation loop */
    public void handleNewMoteOutput(Node mote, String id, long time, String msg) {
        try {
            if (scriptThread == null ||
                    !scriptThread.isAlive()) {
                return;
            }

            /* Update script variables */
            engine.put("mote", mote);
            engine.put("id", id);
            engine.put("time", time);
            engine.put("msg", msg);

            stepScript();
        } catch (UndeclaredThrowableException e) {
            logger.error("Exception: " + e.getMessage(), e);
            stopSimulation();
        }
    }

    private void stopSimulation() {
        /* no more scheduling of this simulation... */

        engine.put("SHUTDOWN", true);

        try {
            if (semaphoreScript != null) {
                semaphoreScript.release(100);
            }
        } catch (Exception e) {
        } finally {
            semaphoreScript = null;
        }
        try {
            if (semaphoreSim != null) {
                semaphoreSim.release(100);
            }
        } catch (Exception e) {
        } finally {
            semaphoreSim = null;
        }

        if (scriptThread != null &&
                scriptThread != Thread.currentThread() /* XXX May deadlock */ ) {
            try {
                scriptThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
            }
        }
        scriptThread = null;
        logOutputListener.stopTick();
    }

    public void startSimulation(String scriptCode) throws ScriptException {
        logOutputListener.startTick();
        if (semaphoreScript != null) {
            logger.warn("Semaphores were not reset correctly");
            semaphoreScript.release(100);
            semaphoreScript = null;
        }
        if (semaphoreSim != null) {
            logger.warn("Semaphores were not reset correctly");
            semaphoreSim.release(100);
            semaphoreSim = null;
        }
        scriptThread = null;

        /* Parse current script */
        CoojaScriptParser parser = new CoojaScriptParser(scriptCode);
        String jsCode = parser.getJSCode();

        timeout = parser.getTimeoutTime();
        if (timeout < 0) {
            timeout = DEFAULT_TIMEOUT;
            logger.info("Default script timeout in " + (timeout/1000) + " ms");
        } else {
            logger.info("Script timeout in " + (timeout/1000) + " ms");
        }

        engine.eval(jsCode);

        /* Setup script control */
        semaphoreScript = new Semaphore(1);
        semaphoreSim = new Semaphore(1);
        engine.put("TIMEOUT", false);
        engine.put("SHUTDOWN", false);
        engine.put("SEMAPHORE_SCRIPT", semaphoreScript);
        engine.put("SEMAPHORE_SIM", semaphoreSim);

        try {
            semaphoreScript.acquire();
        } catch (InterruptedException e) {
            logger.error("Error when creating engine: " + e.getMessage(), e);
        }
        ThreadGroup group = new ThreadGroup("script") {
            public void uncaughtException(Thread t, Throwable e) {
                while (e.getCause() != null) {
                    e = e.getCause();
                }
                if (e.getMessage() != null &&
                        e.getMessage().contains("test script killed") ) {
                    /* Ignore normal shutdown exceptions */
                } else {
                    logger.error("Script error:", e);
                }
            }
        };
        scriptThread = new Thread(group, new Runnable() {
            public void run() {
                /*logger.info("test script thread starts");*/
                try {
                    ((Invocable)engine).getInterface(Runnable.class).run();
                } catch (RuntimeException e) {
                    Throwable throwable = e;
                    while (throwable.getCause() != null) {
                        throwable = throwable.getCause();
                    }

                    if (throwable.getMessage() != null &&
                            throwable.getMessage().contains("test script killed") ) {
                        logger.info("Test script finished");
                    } else {
                        logger.error("Script error:", e);
                        stopSimulation();
                    }
                }
                /*logger.info("test script thread exits");*/
            }
        });
        scriptThread.start(); /* Starts by acquiring semaphore (blocks) */
        while (!semaphoreScript.hasQueuedThreads()) {
            Thread.yield();
        }

        /* Create script output logger */
        engine.put("log", new CoojaScriptLog());

        Hashtable<Object, Object> hash = new Hashtable<Object, Object>();
        engine.put("global", hash);
//        engine.put("sim", simulation);
        engine.put("msg", new String(""));

        scriptMote = new CoojaScriptMote();
        engine.put("node", scriptMote);
    }
    
    
    public static void main(String[] args) throws IOException, ScriptException {
        Simulator simulator = new Simulator();
        RadioMedium radioMedium = new NullRadioMedium();
        radioMedium.setSimulator(simulator);
        simulator.setRadioMedium(radioMedium);
        Server server = new Server(Simulator.DEFAULT_PORT);
        server.setSimulator(simulator);
        simulator.setServer(server);
        server.start();

        CoojaScriptEngine engine = new CoojaScriptEngine(simulator);
        /* When should the script start??? */
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringBuffer code = new StringBuffer();
        String firstLine = reader.readLine();
        System.out.println("**** First Line:" + firstLine);
        String[] parts = firstLine.split(":");
        /* Number of lines of code to read */
        System.out.println("**** Part:" + parts[1]);
        int lines = Integer.parseInt(parts[1]);
        while(lines >= 0) {
            String line = reader.readLine();
            System.out.println("Line:" + line);
            code.append(line + "\n");
            lines--;
        }
        System.out.println("CODE:" + code.toString());
        
        engine.startSimulation(code.toString());
        
    }
}
