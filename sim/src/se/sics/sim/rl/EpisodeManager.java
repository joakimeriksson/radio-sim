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

import se.sics.sim.core.Simulator;
import se.sics.sim.net.PolicyNode;
import se.sics.sim.core.Node;

/**
 */
public class EpisodeManager {

    private static EpisodeManager defaultManager;

    public static EpisodeManager getDefault() {
        return defaultManager;
    }

    public static void setDefault(EpisodeManager manager) {
        defaultManager = manager;
    }

    private EpisodeStat stat;
    private Simulator simulator;

    private PolicyNode[] nodes;
    private int nodeCount;

    private double defaultEpsilon = 0.01;
    private double epsilonDecayFactor = 0.999;

    private double epsilon = defaultEpsilon;

    private PolicyManager policyManager;

    public EpisodeManager() {
    }

    public void init(Simulator sim) {
        this.simulator = sim;

        Node[] allNodes = sim.getNodes();
        stat = new EpisodeStat(allNodes.length);

        if (allNodes != null && allNodes.length > 0) {
            this.nodes = new PolicyNode[allNodes.length];
            for (int i = 0, n = allNodes.length; i < n; i++) {
                if (allNodes[i] instanceof PolicyNode) {
                    this.nodes[nodeCount++] = (PolicyNode) allNodes[i];
                }
            }
            if (nodeCount > 0) {
                this.policyManager = new PolicyManager();
                this.policyManager.initManager(nodes[0]);
                this.policyManager.startManager(nodes[0]);
                for (int i = 0; i < nodeCount; i++) {
                    nodes[i].setPolicyManger(this.policyManager);
                }
            } else {
                System.out.println("EpisodeManager found no policy nodes");
            }
        }
    }

    public void finish(Simulator sim) {
        nextEpisode();
        if (policyManager != null && nodeCount > 0) {
            policyManager.stopManager(nodes[0]);
            policyManager.printPolicy(nodes[0]);
        }
    }

    public void nextEpisode() {
        if (nodeCount == 0)
            return;

        for (int i = 0; i < nodeCount; i++) {
            nodes[i].updateEpisodeStat(stat);
        }

        // Do some stuff...
        // That gets in all the datat from all the nodes and then
        // learns from the utility
        // if (isPolicyDriven) {
        stat.print();
        // }

        // if (!isPolicyDriven) {
        if (policyManager != null) {
            double utility = stat.getUtility();
            policyManager.updateEpisode(utility);
            policyManager.improvePolicy();
            // for (int i = 0; i < nodeCount; i++) {
            // nodes[i].updateEpisode(utility);
            // nodes[i].improvePolicy();
            // }
            policyManager.nextEpisode();
        }

        stat.nextEpisode();

        // Allow nodes to configure!
        for (int i = 0; i < nodeCount; i++) {
            nodes[i].runPolicy();
        }

        // if ((stat.getEpisode() % 10) == 0) {
        // isPolicyDriven = !isPolicyDriven;
        // }

        // if (!isPolicyDriven && nodeCount > 0) {
        // if (Math.random() < epsilon) {
        // this.nodes[(int) (Math.random() * nodeCount)].performExperiment();
        // }
        // // epsilon *= epsilonDecayFactor;
        // }
    }

} // EpisodeManager
