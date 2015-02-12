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
 * $Id: EpisodeManager.java,v 1.6 2007/06/01 11:57:38 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * EpisodeManager
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Thu May 31 14:31:49 2007
 * Updated : $Date: 2007/06/01 11:57:38 $
 *           $Revision: 1.6 $
 */
package se.sics.sim.rl;

import se.sics.sim.core.Node.PolicyMode;
import se.sics.sim.core.Simulator;
import se.sics.sim.util.ActionWrapper;

/**
 */
public class BulkEpisodeManager {

    private static BulkEpisodeManager defaultManager;

    private Simulator simulator;

    // private CC2420Node[] nodes;
    // private int nodeCount;

    // private double defaultEpsilon = 0.01;
    // private double epsilonDecayFactor = 0.99999;
    //
    // private double epsilon = defaultEpsilon;

    private BulkPolicyManager policyManager;
    // private int currentRound = 0;

    private int changingNodeForThisRound = -1;
    private int decisionTimeSlotForThisRound;
    private int numberOfDecisions = 2;

    // private int loopCount;

    private int debug = 1;

    private PolicyMode policyMode;

    private int episodeCount = 0;

    public BulkEpisodeManager() {
    }

    public static BulkEpisodeManager getDefault() {
        return defaultManager;
    }

    public static void setDefault(BulkEpisodeManager manager) {
        defaultManager = manager;
    }

    public void init(Simulator sim, PolicyMode policyMode) {
        this.simulator = sim;
        System.out.println("BulkEpisodeManager does init");
        this.policyManager = new BulkPolicyManager();
        this.policyManager.initManager(policyMode);
        this.policyManager.startManager();
        this.policyMode = policyMode;
        System.out.println("BulkEpisodeManager started in policy mode " + policyMode);

    }

    public void finish(double utility, int[] randomStateActionPairs, Simulator sim) {
        nextEpisode(utility, randomStateActionPairs);
        this.simulator = sim;
    }

    public void nextEpisode(double utility, int[] randomStateActionPairs) {

        if (policyMode != PolicyMode.HARDCODED) {

            changingNodeForThisRound = (int) (Math.random() * simulator.getNodes().length); // -1
            decisionTimeSlotForThisRound = (int) (Math.random() * numberOfDecisions); // -1
            if (1 < debug) {
                System.out.println("Next episode! Changing node for this round is " + changingNodeForThisRound
                        + " at stop " + decisionTimeSlotForThisRound);
            }

            // loopCount = 0;
            policyManager.updateEpisode(utility);
            policyManager.improvePolicy(randomStateActionPairs);

            policyManager.nextEpisode();
        } else if (0 < debug) {
            System.out.println(episodeCount + " Adding utility from fixed policy " + utility);
        }
        episodeCount++;
    }

    public int getPolicyAction(int state, int nodeId, int decisionTimeSlot) {

        switch (policyMode) {
        case HARDCODED:
            return policyManager.getPolicyAction(state, nodeId, false);
        case RANDOM_MOVES:
            if (nodeId == changingNodeForThisRound && decisionTimeSlot == decisionTimeSlotForThisRound) {
                return policyManager.getPolicyAction(state, nodeId, true);
                // loopCount++;
            }
            // no break! continue to:
        case RANDOM_POLICY:
            // at this point we don't want randomness
            return policyManager.getPolicyAction(state, nodeId, false);

        }
        System.err.println("Something went wrong");
        return -1;
        // if(0 < debug) {
        // System.out.println(nodeId + " is in state " +
        // policyManager.getBulkStateAsString(state) + " = " + state +
        // " at time " + decisionTimeSlotForThisRound+ " and is told to " +
        // policyManager.getBulkActionAsString(action) + " = " + action);
        // }
        // return action;
    }

    public ActionWrapper getWrappedPolicyAction(int state, int nodeId, int decisionTimeSlot) {

        return policyManager.getWrappedPolicyAction(state, nodeId, true);
    }

    public void updateVisitCount(int state, int action) {
        policyManager.updateVisitCount(state, action);
    }

    public void printPolicy() {
        policyManager.printPolicy();
    }

    public String getPolicyAsText() {
        return policyManager.getPolicyAsText();
    }

    public String getPolicy() {
        return policyManager.getPolicy();
    }

    public String getBulkStateAsString(int state) {
        return policyManager.getBulkStateAsString(state);

    }

    public String getBulkActionAsString(int action) {
        return policyManager.getBulkActionAsString(action);
    }

    public void setInitialPolicy(int[] initialPolicy) {
        policyManager.setInitialPolicy(initialPolicy);

    }

} // EpisodeManager
