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
import se.sics.sim.net.PolicyNode;

/**
 */
public class PolicyManager {

    private static final boolean FIRST_VISIT = true;

    private int[] policy;
    private boolean isPolicyDriven;

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

    private double defaultEpsilon = 0.05;
    private double epsilonDecayFactor = 0.999;

    private double epsilon = defaultEpsilon;

    public PolicyManager() {
    }

    public boolean isPolicyDriven() {
        return isPolicyDriven;
    }

    public void initManager(PolicyNode node) {
        int totalState = node.getStateCount();
        int actionCount = node.getActionCount();
        qValue = new double[totalState][actionCount];
        qValueSum = new double[totalState][actionCount];
        qCount = new int[totalState][actionCount];
        qVisit = new int[totalState];
        policy = new int[totalState];
        if (FIRST_VISIT) {
            lastUpdated = new int[totalState][actionCount];
        }
    }

    public void startManager(PolicyNode node) {
        episodeSize = 0;
        if (FIRST_VISIT) {
            lastUpdatedCount = 0;
        }
    }

    public void stopManager(PolicyNode node) {
    }

    public void nextEpisode() {
        episodeSize = 0;
        if (FIRST_VISIT) {
            lastUpdatedCount++;
        }
    }

    public int getPolicyAction(PolicyNode node) {
        int state = node.getState();
        int action;
        if (!isPolicyDriven && Math.random() < epsilon) {
            // System.out.println("Performing Random action for node: " +
            // node.getNodeID());
            action = (int) (Math.random() * node.getActionCount());
        } else {
            action = policy[state];
        }

        if (!isPolicyDriven && (!FIRST_VISIT || lastUpdated[state][action] < lastUpdatedCount)) {
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
        return action;
    }

    // Each node learning its own policy, evaluated together....
    public void updateEpisode(double utility) {
        for (int i = 0, n = episodeSize; i < n; i += 2) {
            int state = episode[i];
            int action = episode[i + 1];
            int count = ++qCount[state][action];
            qVisit[state]++;
            qValueSum[state][action] += utility;
            qValue[state][action] = qValueSum[state][action] / count;
        }
        // epsilon *= epsilonDecayFactor;
    }

    // see page 624 in R N :)
    public boolean improvePolicy() {
        boolean policyChanged = false;
        for (int j = 0, m = episodeSize; j < m; j += 2) {
            int state = episode[j];
            double[] qStateValue = qValue[state];

            double bestValue = qStateValue[0];
            int bestAction = 0;
            for (int i = 1, n = qStateValue.length; i < n; i++) {
                if (qStateValue[i] > bestValue) {
                    bestValue = qStateValue[i];
                    bestAction = i;
                }
            }
            if (policy[state] != bestAction) {
                policy[state] = bestAction;
                policyChanged = true;
            }
        }
        return policyChanged;
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

    public void printPolicy(PolicyNode node) {
        // Only print visited states
        for (int i = 0, n = policy.length; i < n; i++) {
            if (policy[i] != 0 || qVisit[i] > 0) {
                System.out.println("State" + node.getStateAsString(i) + " => " + node.getActionAsString(policy[i]));
            }
        }
    }

} // PolicyManager
