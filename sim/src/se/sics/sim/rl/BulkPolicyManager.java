/*
 * Copyright (c) 2007, SICS AB.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 * 3. The name of the author may not be used to endorse or promote
 *    products derived from this software without specific prior
 *    written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * $Id: PolicyManager.java,v 1.6 2007/06/01 13:35:59 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * PolicyManager
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Thu May 31 19:05:01 2007
 * Updated : $Date: 2007/06/01 13:35:59 $
 *           $Revision: 1.6 $
 */
package se.sics.sim.rl;

import java.util.Arrays;
import se.sics.sim.core.Node.PolicyMode;
import se.sics.sim.util.ActionWrapper;

/**
 */
public class BulkPolicyManager {

    private static final boolean FIRST_VISIT = true;

    private int[] policy;
    private int[] policyBeingEvaluated;

    private PolicyMode policyMode;

    private double initialValue = 0.0;

    private double[][] qValueSum;
    private double[][] qValue;
    private int[][] qCount;
    private int[] qVisit;

    private int[][] lastUpdated;
    private int lastUpdatedCount = 0;

    // Episode of "state -> action" moves
    private int[] episode = new int[1000 * 2];
    private int episodeSize;

    private double defaultEpsilon = 0.06;
    // set epsilonDecayFactor to 1 to avoid declining prob. of random action
    private double epsilonDecayFactor = 0.999;

    private double epsilon = defaultEpsilon;

    private String[] states = { "BulkModeOff, XMACOff, SendrateLow", "BulkModeOff, XMACOff, SendrateHigh",
            "BulkModeOff, XMACOn, SendrateLow", "BulkModeOff, XMACOn, SendrateHigh", "BulkMode, XMACOff, SendrateLow",
            "BulkMode, XMACOff, SendrateHigh", "BulkMode, XMACOn, SendrateLow", "BulkMode, XMACOn, SendrateHigh", };

    private String[] actions = { "No action", "Increase sendrate", "Decrease sendrate", "Turn off XMAC", "Turn on XMAC" };

    private int[] policyToEvaluate = {
            // tri-ditto
            4, 2, 0, 3, 0, 2, 3, 0
    // di-ditto
    // 0, 4, 0, 4, 0, 2, 3, 0

    // ditto
    /*
     * 0, 4, 0, 2, 4, 2, 1, 3
     */
    // nyskum f 5n:
    /*
     * 0, 3, 0, 0, 0, 3, 0, 4
     */
    // Step one:
    /*
     * 0, 0, 0, 0, 1, 0, 3, 0
     */
    // 1.763168972E9 vid 5 ndoer??? jao
    /*
     * 4, 4, 0, 0, 1, 1, 3, 0
     */
    // 7.84051776E8 vid 5 ndoer???
    /*
     * 0, 4, 0, 0, 0, 0, 1, 3
     */
    // skum = 8.75...E8 vid 5 noder 5.27...E8 vid 3
    /*
     * 4, 0, 0, 0, 0, 0, 3, 2
     */
    // skum
    /*
     * 0, 0, 0, 2, 0, 1, 3, 3
     */
    // baaad
    /*
     * 0, 0, 0, 0, 1, 1, 3, 0
     */
    // baaad
    /*
     * 0, 0, 0, 0, 1, 3, 3, 0
     */
    // not so baaad
    /*
     * 0, 0, 0, 0, 0, 0, 0, 0
     */
    // optimal
    /*
     * 4, 2, 0, 2, 1, 0, 3, 3
     */
    // time-optimal
    /*
     * 0, 0, 0, 0, 1, 0, 3, 0
     */
    // energy-waister
    /*
     * 4, 2, 0, 1, 1, 0, 3, 2
     */
    //

    };

    private int debug = 1;

    private int initNullValues = 0;

    private int episodeCount = 0;

    public BulkPolicyManager() {
    }

    public boolean isPolicyDriven() {
        return policyMode == PolicyMode.HARDCODED;
    }

    public void initManager(PolicyMode policyMode) {
        this.policyMode = policyMode;
        switch (policyMode) {
        case HARDCODED:
            policy = policyToEvaluate;
            System.out.println("Evaluating policy " + Arrays.toString(policy));
            break;
        case RANDOM_MOVES:

            // no break!
        case RANDOM_POLICY:
            int totalState = states.length;
            int actionCount = actions.length;
            qValue = new double[totalState][actionCount];
            qValueSum = new double[totalState][actionCount];
            qCount = new int[totalState][actionCount];
            qVisit = new int[totalState];
            policy = new int[totalState];
            // System.out.println("initialPolicy is " +
            // sim.getProperty("policy.bestPolicy"));
            if (FIRST_VISIT) {
                lastUpdated = new int[totalState][actionCount];
            }
            break;

        }
    }

    public void startManager() {
        episodeSize = 0;
        if (FIRST_VISIT) {
            lastUpdatedCount = 0;
        }
    }

    public void nextEpisode() {
        episodeSize = 0;
        if (FIRST_VISIT) {
            lastUpdatedCount++;
        }
        episodeCount++;
    }

    public int getPolicyAction(int state, int nodeId, boolean random) {
        // TODO
        // System.out.println("PolicyManager is called");
        int action;
        double rand = random ? Math.random() : 1;
        if (policyMode != PolicyMode.HARDCODED && rand < epsilon) {
            action = (int) (Math.random() * actions.length);
            if (1 < debug) {
                System.out.println(nodeId + " performing random action for " + state + "-" + action + " = "
                        + getBulkStateAsString(state) + "-" + getBulkActionAsString(action));
            }
        } else {
            action = policy[state];
            if (2 < debug) {// || state == 0) {
                System.out.println(nodeId + " picking known action for " + state + "-" + action + " = "
                        + getBulkStateAsString(state) + "-" + getBulkActionAsString(action) + " random was " + random);
            }
        }

        if (!(policyMode == PolicyMode.HARDCODED) &&
        /*
         * in RANDOM_POLICY mode, only record visited states when you evaluate
         * current policy, not when you generate new random moves!
         */
        !(random && policyMode == PolicyMode.RANDOM_POLICY)
                && (!FIRST_VISIT || lastUpdated[state][action] < lastUpdatedCount)) {

            updateVisitCount(state, action);

        }
        return action;
    }

    /*
     * We assume this method is only called in policyMode "RANDOM_POLICY"
     */
    public ActionWrapper getWrappedPolicyAction(int state, int nodeId, boolean random) {

        int action;
        double rand = random ? Math.random() : 1;
        if (rand < epsilon) {
            action = (int) (Math.random() * actions.length);
            if (1 < debug) {
                System.out.println(nodeId + " performing random wrapped action for " + state + "-" + action + " = "
                        + getBulkStateAsString(state) + "-" + getBulkActionAsString(action));
            }
            // not yet restore-time, more random moves to make!
            return new ActionWrapper(action, 0 /* DO_RANDOM = 0 */);
        }

        action = policy[state];
        if (1 < debug) {// || state == 0) {
            System.out.println(nodeId + " picking known wrapped action for " + state + "-" + action + " = "
                    + getBulkStateAsString(state) + "-" + getBulkActionAsString(action) + " random was " + random);
        }
        // NOW restore-time, no more random moves to make!
        // TODO:
        // evaluate!
        // updateVisitCount(state, action);
        return new ActionWrapper(action, 42 /* CHECK_NEED_TO_RESTORE = 42 */);
    }

    public void updateVisitCount(int state, int action) {

        if (episodeSize == episode.length) {
            int[] tmp = new int[episodeSize + 1000 * 2];
            System.arraycopy(episode, 0, tmp, 0, episodeSize);
            episode = tmp;
        }
        episode[episodeSize] = state;
        episode[episodeSize + 1] = action;
        episodeSize += 2;
        if (FIRST_VISIT) {
            lastUpdated[state][action] = lastUpdatedCount;
        }
    }

    // Each node learning its own policy, evaluated together....
    public void updateEpisode(double utility) {
        if (0 < debug) {
            System.out.println(episodeCount + " Adding utility " + utility + " " + getPolicy());
        }
        if (-1 < initNullValues) {
            initNullActionValues(utility, initNullValues);
            initNullValues = -1;
        }

        // exclude null-action?
        for (int i = 0, n = episodeSize; i < n; i += 2) {
            int state = episode[i];
            int action = episode[i + 1];
            int count = ++qCount[state][action];
            qVisit[state]++;
            // if(0 < action) {
            qValueSum[state][action] += utility;
            qValue[state][action] = qValueSum[state][action] / count;
            if (1 < debug) {
                System.out.println("new value of state-action-pair " + state + "-" + action + " is "
                        + qValue[state][action] + " after visiting " + count + " times");
            }
            // }
        }
        if (epsilonDecayFactor < 1) {
            epsilon *= epsilonDecayFactor;
        }
    }

    private void initNullActionValues(double utility, int trueValueIndicator) {
        // the value of action 0 is at least this
        if (trueValueIndicator == 1) {

            for (int anyState = 0; anyState < states.length; anyState++) {
                qValue[anyState][0] = utility;
            }
            System.out.println("Init null-action-value to " + utility);
        } else {
            for (int anyState = 0; anyState < states.length; anyState++) {
                qValue[anyState][0] = utility / 2;
            }
            System.out.println("Init null-action-value to " + (utility / 2));
        }

    }

    // see page 624 in R N :)
    public boolean improvePolicy(int[] randomStateActionPairs) {
        boolean policyChanged = false;
        int state;
        int bestAction;
        for (int j = 0, m = episodeSize; j < m; j += 2) {
            state = episode[j];
            // System.out.print(state + " ");
            double[] qStateValue = qValue[state];

            // when just picking the first, there is no guarantee that this is
            // the
            // value achieved by the current policy - the current policy
            // might be much worse!

            // double bestValue = qStateValue[0];
            double bestValue = qStateValue[policy[state]];
            double bestIValue = bestValue;
            bestAction = -1;

            // start from i = 0 to allow no action
            for (int i = 0, n = qStateValue.length; i < n; i++) {
                if (qStateValue[i] > bestValue) {
                    bestValue = qStateValue[i];
                    bestAction = i;
                }
            }
            if (episodeCount % 500 == 0) {
                // stats:
                System.out.println("The best action for state "
                        + getBulkStateAsString(state)
                        + " is "
                        + bestValue
                        + " achieved when doing "
                        + (-1 < bestAction ? "new action " + getBulkActionAsString(bestAction) : "policy action "
                                + getBulkActionAsString(policy[state])));
            }
            if (-1 < bestAction && policy[state] != bestAction) {
                if (1 < debug) {
                    System.out.println("\n" + episodeCount
                            + " PolicyManager is updating policy. The best state-action is " + state + "-" + bestAction
                            + " = " + getBulkStateAsString(state) + "-" + getBulkActionAsString(bestAction)
                            + " with value " + bestValue + " compared with best prev action " + policy[state] + " "
                            + bestIValue);
                }
                policy[state] = bestAction;
                policyChanged = true;
            } else {
                // System.out.println();
            }
        }

        // here we would be done, if we are not doing a random policy
        if (policyMode != PolicyMode.RANDOM_POLICY) {
            return policyChanged;
        }
        // System.out.println("PolicyManager is given these random moves: " +
        // Arrays.toString(randomStateActionPairs));
        /*
         * the simplest sol: last update wins
         */
        int i = 0, previousBestAction;
        while (-1 < randomStateActionPairs[i]) {
            state = randomStateActionPairs[i];
            bestAction = randomStateActionPairs[i + 1];
            previousBestAction = policy[state];
            if (bestAction != previousBestAction) {
                if (1 < debug) {
                    System.out.println("PolicyManager is testing a new random policy. state-action is " + state + "-"
                            + bestAction + " = " + getBulkStateAsString(state) + "-"
                            + getBulkActionAsString(bestAction) + " vs old: " + policy[state]);
                }
                policy[state] = bestAction;
            }
            i += 2;
        }// end while

        return true;

    }

    public void clearPolicy() {
        Arrays.fill(policy, 0);
        epsilon = defaultEpsilon;
        clearValues();
    }

    protected void clearValues() {
        // Clear learned values
        int number = initialValue != 0.0 ? 1 : 0;
        for (int i = 0, n = qValue.length; i < n; i++) {
            double[] q = qValue[i];
            int[] qc = qCount[i];
            qVisit[i] = 0;
            for (int k2 = 0, km2 = q.length; k2 < km2; k2++) {
                q[k2] = initialValue;
                qc[k2] = number;
            }
        }
    }

    public String getPolicyAsText() {
        String result = "";
        for (int i = 0, n = policy.length; i < n; i++) {
            if (policyMode == PolicyMode.HARDCODED) {
                result += "State" + getBulkStateAsString(i) + " => " + getBulkActionAsString(policy[i]) + "\n";

            } else {
                if (policy[i] != 0 || qVisit[i] > 0) {
                    result += "State" + getBulkStateAsString(i) + " => " + getBulkActionAsString(policy[i]) + "\n";
                }
            }
        }
        return result;
    }

    public String getPolicy() {
        return Arrays.toString(policy);
    }

    public void printPolicy() {
        // Only print visited states
        for (int i = 0, n = policy.length; i < n; i++) {
            if (policy[i] != 0 || qVisit[i] > 0) {
                System.out.println("State" + getBulkStateAsString(i) + " => " + getBulkActionAsString(policy[i]));
            }
        }
    }

    public String getBulkStateAsString(int state) {
        return states[state];

    }

    public String getBulkActionAsString(int action) {
        return actions[action];
    }

    public void setInitialPolicy(int[] initialPolicy) {
        initNullValues = 1;
        policy = initialPolicy;

    }

} // PolicyManager
