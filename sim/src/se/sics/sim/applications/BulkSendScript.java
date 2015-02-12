package se.sics.sim.applications;

import se.sics.sim.core.Simulator;
import se.sics.sim.interfaces.MessageToPacketLayer;

public class BulkSendScript {

    static Simulator simulator;

    static boolean finishedWithoutError = false;

    static final int PLAIN_LOOP = 1;
    static final int DIVIDE_AND_CONQUER = 2;
    static private int loopMode;

    private static int numberOfNodes = 5;

    // private static double bestUtilitySoFar;

    public static void main(String[] args) {

        String[] simArgs = {
        /* 0 */"simulation.nodeLayout=LineSetup",
        /* 1 */"simulation.nodeType=CC2420Node",
        /* 2 */"simulation.protocolType=XMAC",
        /* 3 */"simulation.applicationType=BulkSend",
        /* 4 */"simulation.policyType=LearningBulkPolicy" // "HardcodedBulkPolicy"
                                                          // //DummyStateListener"//LearningBulkPolicy"
                                                          // //DummyStateListener" //"" //"HardcodedBulkPolicy" //"LearningBulkPolicy"
                                                          // //
                ,
                /* 5 */"network.numberOfNodes=" + numberOfNodes,
                /* 6 */"application.numberOfTestMessages=1",
                /* 7 */"bulkSend.packetSendInterval",
                /* 8 */"simulation.maxUtility=",
                /* 9 */"simulation.stopTime=",
                /* 10 */"policy.initialPolicy=",
                /* 11 */"simulation.resetFlag=",
                /* 12 */"bulkSend.accMode=1",
                /* 13 */"simulation.maxPenalty=" + (-(Integer.MAX_VALUE / 50)), ""
        /*
         * "bulkSend.packetSendInterval=???", "bulkSend.accMode=1",
         * "simulation.maxUtility=", "simulation.stopTime=",
         * "policy.initialPolicy=",
         */
        };

        loopMode = PLAIN_LOOP;
        int maxInnerLoopCount;
        int maxOuterLoopCount = 5;
        long maxPacketSendInterval = 1494139; // 6000000L;// 50000000L;
        long packetSendInterval;

        // if(simArgs[3].equals("bulkSend.accMode=3")) { maxInnerLoopCount = 1;
        // packetSendInterval = 0; } else
        if (loopMode != PLAIN_LOOP) {
            maxInnerLoopCount = 500; // 50;
            packetSendInterval = maxPacketSendInterval;

            simArgs[8] = "simulation.maxUtility=" + MessageToPacketLayer.MAX_TIME_TO_TRANSFER_ONE_MESSAGE;
            simArgs[9] = "simulation.stopTime="
                    + (2 * (MessageToPacketLayer.MESSAGE_LENGTH / MessageToPacketLayer.APPLICATION_PACKET_SIZE) * packetSendInterval);// Should
                                                                                                                                      // be
                                                                                                                                      // the
                                                                                                                                      // same
                                                                                                                                      // as
                                                                                                                                      // STOP_TIME!
            // hmm
        } else { // RL-style!
            maxInnerLoopCount = 1000;// 000;//500; //750;//000; //10000; //000;
                                     // //60000; //700; //10000; //50;
            packetSendInterval = 1494139;
            long maxU;
            if (numberOfNodes < 4) {
                packetSendInterval = 534000; // 1494139
                maxU = 2500 * packetSendInterval;
            } else {
                packetSendInterval = 1494139;
                maxU = 1000 * packetSendInterval;
            }
            simArgs[8] = "simulation.maxUtility=" + maxU; // 1302314315;
                                                          // //-(MessageToPacketLayer.MAX_TIME_TO_TRANSFER_ONE_MESSAGE
                                                          // +
                                                          // packetSendInterval);
            simArgs[9] = "simulation.stopTime="
                    + (2 * (MessageToPacketLayer.MESSAGE_LENGTH / MessageToPacketLayer.APPLICATION_PACKET_SIZE) * packetSendInterval);// Should
                                                                                                                                      // be
                                                                                                                                      // the
                                                                                                                                      // same
                                                                                                                                      // as
                                                                                                                                      // STOP_TIME!
        }

        long lastSuccesfulPacketSendInterval = 0;

        Simulator simulator = new Simulator();
        int acceptableSlack = 10;
        int lastStep = (int) (packetSendInterval / 2);
        int innerLoopCount = 0, outerLoopCount = 0;
        long startTime = System.currentTimeMillis();
        String initialPolicy = null; // "[0, 4, 0, 4, 0, 2, 3, 0]";//"[0, 4, 0, 2, 4, 2, 1, 3]";//null;
                                     // //"[0, 0, 0, 0, 0, 2, 3, 0]"; //null;
                                     // //"[4, 2, 0, 2, 1, 0, 3, 3]";
        // 0, 0, 0, 0, 0, 0, 0, 0]";// null; //1 0 3

        do {

            simArgs[10] = "policy.initialPolicy=" + initialPolicy;
            simArgs[11] = "simulation.resetFlag=true";
            outerLoopCount++;
            innerLoopCount = 0;

            do {
                innerLoopCount++;

                // System.out.println("Starting. Testing packetSendInterval = "
                // + packetSendInterval);
                simArgs[7] = "bulkSend.packetSendInterval=" + packetSendInterval;
                Simulator.main(simArgs);
                simArgs[11] = "simulation.resetFlag=false";
                try {
                    // let the processor cool down...
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                finishedWithoutError = simulator.getStatus();
                if (loopMode == DIVIDE_AND_CONQUER) {
                    System.out.println("result was " + finishedWithoutError + " after trying packetSendInterval "
                            + packetSendInterval);
                }
                if (loopMode == DIVIDE_AND_CONQUER) {
                    if (finishedWithoutError) {
                        // update last known successful.
                        lastSuccesfulPacketSendInterval = packetSendInterval;
                        packetSendInterval -= lastStep;

                    } else {
                        // increase the interval
                        packetSendInterval += lastStep;
                        if (maxPacketSendInterval < packetSendInterval) {
                            // we failed! give up
                            lastStep = 2;
                        }
                    }
                    // half the delta:
                    lastStep /= 2; // /= 2;
                } // endif(loopMode == DIVIDE_AND_CONQUER) {
                else {
                    if (innerLoopCount % 500 == 0) {
                        System.out.println(innerLoopCount);
                    }
                }

            } while (acceptableSlack < lastStep && innerLoopCount < maxInnerLoopCount);

            // initialPolicy = Arrays.toString((long[])
            // simulator.getProperty("policy.bestPolicy"));
            initialPolicy = (String) simulator.getProperty("policy.bestPolicy");
            // bestUtilitySoFar = Double.parseDouble((String)
            // simulator.getProperty("policy.bestGlobalUtility"));

            // simulator.getProperty("policy.bestGlobalUtility")
        } while (outerLoopCount < maxOuterLoopCount);

        System.out.println("Finally done after " + innerLoopCount + " loops.");
        if (loopMode != PLAIN_LOOP) {
            System.out.println("Resulting lastSuccesfulPacketSendInterval is " + lastSuccesfulPacketSendInterval);
        } else {

            System.out.println("Resulting best policy is\n" + simulator.getProperty("policy.bestPolicyAsText")
                    + " which gives an utility of " + simulator.getProperty("policy.bestGlobalUtility"));

        }
        long endTime = System.currentTimeMillis();
        System.out.println("Total time spent is " + ((endTime - startTime) / 1000) + " sec");
        System.out.println("The last policy is:\n" + simulator.getResult(0) + " which gives an utility of "
                + simulator.getProperty("policy.lastGlobalUtility"));
        ;
    }
}
