package se.sics.sim.policy;

import java.util.Arrays;
import se.sics.sim.core.Node;
import se.sics.sim.core.Simulator;
import se.sics.sim.core.Node.PolicyMode;
import se.sics.sim.core.Node.State;
import se.sics.sim.interfaces.StateListener;
import se.sics.sim.net.CC2420Node;
import se.sics.sim.rl.BulkEpisodeManager;
import se.sics.sim.rl.MakePolicyChangeEvent;
import se.sics.sim.util.ActionWrapper;

public class LearningBulkPolicy implements StateListener {

    // TODO
    // int off;
    BulkEpisodeManager bulkEpisodeManager;

    private int hardcodedLongBetweenPacketDelay = 534000;
    private int hardcodedShortBetweenPacketDelay = 55000;
    // to find 'time optimal' hardcodedShortBetweenPacketDelay / 50 seems "ok"
    private int defaultPolicyPenalty = hardcodedShortBetweenPacketDelay / 25; // has
                                                                              // been
                                                                              // 50
                                                                              // for
                                                                              // a
                                                                              // long
                                                                              // time;
                                                                              // //
                                                                              // 2//was
    // 10

    private Simulator simulator;

    PolicyState[] policyStates;
    private int debug = 1;

    private PolicyMode policyMode = PolicyMode.RANDOM_POLICY; // RANDOM_POLICY;
    public static long POLICY_DELAY = 5000; // 5000; //10000 // 500

    private int[] randomStateActionPairs;
    private int positionForNextRandomStateActionPair;

    private int numberOfNodesDefault = 3;
    private int numberOfNodes = -1;

    private static boolean started = false;

    // private int lastAction = -1;

    public LearningBulkPolicy() {
        System.out.println("LearningBulkPolicy is instantiated at startup");
        bulkEpisodeManager = new BulkEpisodeManager();
        // if(policyMode == PolicyMode.RANDOM_POLICY) {
        randomStateActionPairs = new int[1000];
        // }

        // for (int i = 0; i < lastActions.length; i++) {
        // lastActions[i] = new ActionWrapper(0, 0);
        // }
        // allCombosAlreadyTried = new HashMap<String, HashSet<String>>();

    }

    @Override
    public void stateChanged(Node source, State stateChangeVariable, int value) {
        stateChanged((CC2420Node) source, stateChangeVariable, value, simulator.getSimulationTime());
    }

    @Override
    public void stateChanged(Node source, State stateChangeVariable, int value, long time) {
        stateChanged((CC2420Node) source, stateChangeVariable, value, time);
    }

    private void stateChanged(CC2420Node source, State stateChangeVariable, int value, long time) {
        // Format: "BulkMode, XMACOn, SendrateHigh",

        if (!started) {
            internalInit();
        }
        // Common actions for all policy modes:
        int nodeID = source.nodeID;
        PolicyState currentPolicyState = policyStates[nodeID];

        // Reading/parsing current state:
        int bulkModeIndicator = source.getProperty(State.BULK);
        int xmacOnIndicator = 0 < source.getProperty(State.MAC_OFF_TIME) ? 1 : 0;
        int sendRateIndicator = isHighSendrate(source.getProperty(State.BETWEEN_PACKET_DELAY));

        String binaryState = "" + bulkModeIndicator + xmacOnIndicator + sendRateIndicator;
        int currentState = Integer.parseInt(binaryState, 2);
        // int timeslotIndicator = 0;
        boolean sameTimeSlot = false;
        sameTimeSlot = 0 <= currentPolicyState.lastStatechangeTime
                && time <= currentPolicyState.lastStatechangeTime + POLICY_DELAY;

        currentPolicyState.lastStatechangeTime = time;

        if (!sameTimeSlot) {
            // we have moved to a new decision point
            currentPolicyState.timeSlot++;
            currentPolicyState.mode = PolicyState.INIT;
        }

        switch (policyMode) {
        case HARDCODED:
            break;
        case RANDOM_MOVES:
            break;
        case RANDOM_POLICY:
            boolean doneRestoring = false;
            switch (currentPolicyState.mode) {
            case PolicyState.INIT:

                currentPolicyState.savedState = currentState;
                // System.out.println(source.nodeID + " saving state " +
                // currentState + " at time " + time);

                break;
            case PolicyState.DO_RANDOM:
                // nuffin?
                break;
            case PolicyState.FOLLOW_POLICY:
                break;
            case PolicyState.RESTORE:
                // FIXME
                doneRestoring = restoreState(source, currentState, binaryState, currentPolicyState, time);
                if (!doneRestoring && 0 < debug) {
                    System.out.println(nodeID + " continues restoring state");
                    return;
                }
                break;

            }
            break;

        }

        int action = 0;

        switch (policyMode) {
        case HARDCODED:
            // no break!
        case RANDOM_MOVES:
            action = bulkEpisodeManager.getPolicyAction(currentState, source.nodeID, currentPolicyState.timeSlot);
            // if(action == 1) {
            // System.out.println(nodeID + " is in state " +
            // bulkEpisodeManager.getBulkStateAsString(currentState) +
            // " and is told to increase sendrate at time " + time);
            // }
            break;

        case RANDOM_POLICY:
            if (currentPolicyState.mode == PolicyState.FOLLOW_POLICY) {
                // generate
                // System.out.println(nodeID + " follow policy");
                action = bulkEpisodeManager.getPolicyAction(currentState, source.nodeID, PolicyState.FOLLOW_POLICY /*
                                                                                                                    * OBS
                                                                                                                    * NOT
                                                                                                                    * READ
                                                                                                                    */);
                // getPolicyAction updates visitcount
                // currentPolicyState.mode = PolicyState.INIT; //only reset when
                // we enter a
                // new timeslot
            } else {
                // System.out.println(nodeID + " allow random move. mode is " +
                // currentPolicyState.mode);
                /*
                 * OBS - getWrappedPolicyAction does not update visitcount,
                 * therefore it must be updated afterwards if the tentative move
                 * is kept (and not reversed)
                 */
                currentPolicyState.lastAction = bulkEpisodeManager.getWrappedPolicyAction(currentState, source.nodeID,
                        PolicyState.DO_RANDOM /* OBS NOT READ */);

                if (currentPolicyState.lastAction.restoreMode != PolicyState.DO_RANDOM) {
                    /*
                     * this means we are done generating random moves. we must
                     * now check if we should restore the state
                     */
                    // TODO
                    if (currentPolicyState.savedState != currentState) {
                        // System.out.println(nodeID + " time to restore from "
                        // + currentState + " to " +
                        // currentPolicyState.savedState);
                        restoreState(source, currentState, binaryState, currentPolicyState, time);
                        // here we should _not_ update visitcount
                    } else {
                        // execute this action & set flag follow
                        action = currentPolicyState.lastAction.action;
                        currentPolicyState.mode = PolicyState.FOLLOW_POLICY;
                        // here we _should_ update visitcount
                        bulkEpisodeManager.updateVisitCount(currentState, action);
                    }

                } else {
                    // perform the random action!
                    action = currentPolicyState.lastAction.action;
                    /*
                     * BUT if the action is 0 we wont come back here, so we must
                     * reset the flags here
                     */
                    if (action == 0) {

                        if (currentPolicyState.savedState != currentState) {
                            restoreState(source, currentState, binaryState, currentPolicyState, time);
                        } else {
                            // execute this action & reset flag(s)
                            // bulkEpisodeManager.updateVisitCount(currentState,
                            // 0); //TODO
                            currentPolicyState.mode = PolicyState.INIT;
                        }
                    }
                    // Record the random moves!
                    currentPolicyState.mode = PolicyState.DO_RANDOM;
                    randomStateActionPairs[positionForNextRandomStateActionPair] = currentState;
                    randomStateActionPairs[positionForNextRandomStateActionPair + 1] = currentPolicyState.lastAction.action;
                    positionForNextRandomStateActionPair += 2;

                }
            }
            break;

        }

        /*
         * Actions:
         * 
         * "No action", "Increase sendrate", "Decrease sendrate",
         * "Turn off XMAC", "Turn on XMAC"
         */
        // TODO
        boolean policyPenalty = true; // lastAction.restoreMode == 0;

        time += POLICY_DELAY;

        MakePolicyChangeEvent actionEvent = null;

        switch (action) {
        case 0:
            // no-op, no cost
            policyPenalty = false;
            break;
        case 1:
            // System.out.println(nodeID + " send faster!");
            // source.setProperty(State.BETWEEN_PACKET_DELAY,
            // hardcodedShortBetweenPacketDelay);
            actionEvent = new MakePolicyChangeEvent(source, State.BETWEEN_PACKET_DELAY,
                    hardcodedShortBetweenPacketDelay, time);
            break;
        case 2:
            // source.setProperty(State.BETWEEN_PACKET_DELAY,
            // hardcodedLongBetweenPacketDelay);
            actionEvent = new MakePolicyChangeEvent(source, State.BETWEEN_PACKET_DELAY,
                    hardcodedLongBetweenPacketDelay, time);
            break;
        case 3:
            // source.setProperty(State.MAC_OFF_TIME, 0);
            actionEvent = new MakePolicyChangeEvent(source, State.MAC_OFF_TIME, 0, time);
            break;
        case 4:
            // source.setProperty(State.MAC_OFF_TIME, 1);
            actionEvent = new MakePolicyChangeEvent(source, State.MAC_OFF_TIME, 1, time);
            break;

        }
        if (policyPenalty) {
            ((CC2420Node) simulator.getNodes()[0]).addUtilityPenalty(defaultPolicyPenalty);
            // source.addUtilityPenalty(defaultPolicyPenalty);
            // System.out.println(source.nodeID + " adding change for time " +
            // time + " to react to " + stateChangeVariable + " " + value +
            // ": set " + actionEvent.state + " to " + actionEvent.value);
            simulator.addEvent(actionEvent);
        }
        // System.out.println(nodeID + " policy does return at t "
        // +simulator.getSimulationTime());

    }

    private boolean restoreState(CC2420Node source, int currentState, String binaryState, PolicyState policyState,
            long time) {

        int nodeID = source.nodeID;
        // System.out.println(nodeID + " restoretime at " + time + ": "
        // + currentState + " vs " + policyState.savedState);

        // int allowRandomMoves = 0;
        // String binaryState = Integer.toBinaryString(currentState);

        if (currentState == policyState.savedState) {
            // We are done restoring the state! now we want to pick the
            // prescribed policy action
            policyState.mode = PolicyState.FOLLOW_POLICY;
            return true;
        }
        policyState.mode = PolicyState.RESTORE;
        MakePolicyChangeEvent actionEvent = null;
        String savedStateString = Integer.toBinaryString(policyState.savedState);
        // add needed padding of binary string:
        if (savedStateString.length() < 3) {
            savedStateString = "0" + savedStateString;
        }
        if (savedStateString.length() < 3) {
            savedStateString = "0" + savedStateString;
        }
        // Now it must be 3 char long!
        // System.out.println(source.nodeID + " The saved state was " +
        // policyState.savedState
        // + " so the string is " + savedStateString + " vs "
        // + binaryState);
        /*
         * We = the policy cannot/should not change the BULK_MODE, (pos = 0) as
         * it should be triggered by the application
         */
        time++; // not to have the exact same timeslot,
        // which would cause the new event to be executed before we are
        // done with this method...

        if (savedStateString.charAt(1) != binaryState.charAt(1)) {
            if (binaryState.charAt(1) == '1') {
                // turn off xmac:
                if (1 < debug)
                    System.out.println(source.nodeID + " adding event to turn off xmac at time " + time);
                actionEvent = new MakePolicyChangeEvent(source, State.MAC_OFF_TIME, 0, time);
            } else {
                // turn on xmac:
                if (1 < debug)
                    System.out.println(source.nodeID + " adding event to turn on xmac at time " + time);
                actionEvent = new MakePolicyChangeEvent(source, State.MAC_OFF_TIME, 1, time);
            }
        } else if (savedStateString.charAt(2) != binaryState.charAt(2)) {

            if (binaryState.charAt(2) == '1') {
                // decrease sendrate
                if (1 < debug)
                    System.out.println(source.nodeID + " adding event to decrease sendrate at time " + time);
                actionEvent = new MakePolicyChangeEvent(source, State.BETWEEN_PACKET_DELAY,
                        hardcodedLongBetweenPacketDelay, time);

            } else {
                // increase sendrate
                if (1 < debug)
                    System.out.println(source.nodeID + " adding event to increase sendrate at time " + time);
                actionEvent = new MakePolicyChangeEvent(source, State.BETWEEN_PACKET_DELAY,
                        hardcodedShortBetweenPacketDelay, time);
            }
        }
        simulator.addEvent(actionEvent);
        return false; // done for this restore-step

    }

    private int isHighSendrate(int value) {
        if (value < hardcodedLongBetweenPacketDelay) {
            return 1;
        }
        return 0;
    }

    // public void reset() {
    // int activeNodeInCurrentEpisode = -1;
    // }
    @Override
    public void finish(Simulator simulator) {
        // bulkEpisodeManager.finish((Double)
        // simulator.getProperty("policy.lastGlobalUtility"));

    }

    @Override
    public void init(Simulator sim) {

        this.simulator = sim;
        System.out.println("LearningPolicy is initialized");
        bulkEpisodeManager.init(sim, policyMode);
        started = false;

    }

    @Override
    public void reinit(Simulator sim) {
        this.simulator = sim;
        policyStates = new PolicyState[numberOfNodes];
        for (int i = 0; i < numberOfNodes; i++) {
            policyStates[i] = new PolicyState();
        }
        // FIXME
        randomStateActionPairs[positionForNextRandomStateActionPair] = -1;
        // end of action-indicator, saves one parameter in method call...
        positionForNextRandomStateActionPair = 0; // reset
        bulkEpisodeManager.finish((Double) simulator.getProperty("policy.lastGlobalUtility"), randomStateActionPairs,
                sim);

    }

    private void internalInit() {
        // FIXME
        started = true;
        String non = (String) simulator.getProperty("network.numberOfNodes");
        if (non != null) {
            numberOfNodes = Integer.parseInt(non);
        } else {
            numberOfNodes = numberOfNodesDefault; // "default"
        }
        policyStates = new PolicyState[numberOfNodes];
        for (int i = 0; i < numberOfNodes; i++) {
            policyStates[i] = new PolicyState();
        }

        System.out.println("LearningBulkPolicy believs there are " + numberOfNodes + " nodes");
        System.out.println("initialPolicy is " + simulator.getProperty("policy.initialPolicy"));
        String initialPolicyString = (String) simulator.getProperty("policy.initialPolicy");
        if (!initialPolicyString.equals("null")) {
            System.out.println("String is " + initialPolicyString.substring(1, initialPolicyString.length() - 1));
            String[] values = initialPolicyString.substring(1, initialPolicyString.length() - 1).split(", ");

            int[] initialPolicy = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                initialPolicy[i] = Integer.parseInt(values[i]);
            }
            System.out.println("initialPolicy after parsing " + Arrays.toString(initialPolicy));
            bulkEpisodeManager.setInitialPolicy(initialPolicy);
        }

    }

    @Override
    public Object getPolicyAsText() {
        return bulkEpisodeManager.getPolicyAsText();
    }

    @Override
    public String getPolicy() {
        return bulkEpisodeManager.getPolicy();
    }

    class PolicyState {
        static final int INIT = -1;
        static final int DO_RANDOM = 0;
        static final int FOLLOW_POLICY = 1;
        static final int RESTORE = 2;
        // CHECK_NEED_TO_RESTORE = 42

        long lastStatechangeTime = -1;
        int timeSlot;
        // To be able to store/restore state:
        int savedState;
        int mode = -1;
        ActionWrapper lastAction;

    }
}
