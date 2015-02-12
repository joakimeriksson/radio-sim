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
 * $Id: PolicyNode.java,v 1.7 2007/06/01 11:57:38 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * PolicyNode
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Fri May 25 15:01:06 2007
 * Updated : $Date: 2007/06/01 11:57:38 $
 *           $Revision: 1.7 $
 */
package se.sics.sim.net;

import se.sics.sim.core.*;
import se.sics.sim.rl.*;

/**
 */
public class PolicyNode extends ExtendedNode {

    public static final boolean DEBUG = false;

    private int[] lastMessage;
    private int neighborCount = 0;

    private int ackCount;
    private int lastRecvID = 0;

    private int maxResends = 4;
    private double threshold = 1.0;
    private boolean isHandlingImportantLinks = true;

    // private static int[] policy;

    // private static double[][] qValueSum;
    // private static double[][] qValue;
    // private static int[][] qCount;
    // private int lastAction;

    private PolicyManager policyManager;

    // Episode related statistics for the reward/utility calculations!
    private boolean floodReceived = false;
    private long lastBytes = 0;

    public void setPolicyManger(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    // Set the neighbors
    protected void init() {
        super.init();

        Link[] neighbors = getLinks();

        // This should also set number of important neighbors!
        // or should we infer that automatically or get it by communication?
        neighborCount = neighbors == null ? 0 : neighbors.length;

        // Create an empty array of message ID's to store which message
        // each neighbor have seen/sent last
        lastMessage = new int[neighborCount];

        // clearQValues();

        initPolicy();
        // // Note: the paper says four to six, and eight or more... where is
        // the 7?
        // // These are fixed in RBP!
        // if (neighborCount >= 4 && neighborCount <= 7) {
        // threshold = 0.66;
        // maxResends = 2;
        // } else if (neighborCount >= 8) {
        // threshold = 0.5;
        // maxResends = 1;
        // } else {
        // threshold = 1.0;
        // maxResends = 3;
        // }
        // updateVisualizer();
    }

    protected void updateVisualizer() {
        if (simulator.visualizer != null) {
            if (importantCount > 0) {
                simulator.visualizer.setNodeProperty(nodeID, "Important Links", "" + importantCount
                        + (isHandlingImportantLinks ? "" : " (ignored)"));
            }
            simulator.visualizer.setNodeProperty(nodeID, "Threshold", "" + (int) (threshold * 100 + 0.5) + '%');
            simulator.visualizer.setNodeProperty(nodeID, "Max Resends", "" + maxResends);
        }
    }

    public void messageReceived(long time, Message message) {
        // System.out.println("Node " + nodeID +
        // ": message received from node: " +
        // src.nodeID + " packetId = " + packetId + " myID: " + lastID);
        MessageNode src = (MessageNode) message.getSource();
        int id = message.getID();
        int type = message.getType();
        boolean send = false;
        if (lastRecvID < id) {
            msgRecv++;
            lastRecvID = id;
        }
        if (type == TYPE_MESSAGE && id > lastSentID) {
            // Only broadcast once!!!
            lastSentID = id;
            ackCount = 0;
            send = true;
            floodReceived = true;
        }

        // Increase the ack on the neighbors if this had not been
        // received or seen by this neighbor before!
        Node[] neighbors = getNeighbors();
        int index = indexOf(neighbors, src);
        if (index < 0) {
            // Not a neighbor: should never happen with bi-directional links
        } else if (lastMessage[index] < id) {
            lastMessage[index] = id;
            ackCount++;
        } else {
            // Might be a resend, send an ack!
            // Can we avoid sendin acks to nodes that we for example
            // have sent this message to? But what if they got it via another
            // way and believe that we are the one node that has not gotten
            // the message?
            if (type == TYPE_MESSAGE) {
                sendAck(time, src, id);
            }
        }

        if (send) {
            if (ackCount < neighborCount) {
                sendBroadcast(time, TYPE_MESSAGE, id);
            } else {
                // Last node does only need to send an ack...
                sendAck(time, src, id);
            }
        }
    }

    // If too few neighbors have acked this message, retransmit!!
    public int transmit(BroadcastEvent evt) {
        // IF less than threshold have acked => resend! || numImportant > 0
        if ((ackCount < threshold * neighborCount) && evt.transmitts <= maxResends) {
            return MSG_SEND;
        }
        // Note: although the paper says "if any important links &&
        // resends < 4; transmit" in Figure 3, the text says "up to 4
        // retries when that downstream node does not ack".
        if (isHandlingImportantLinks && importantCount > 0 && evt.transmitts <= 4) {
            int id = evt.getID();
            for (int i = 0; i < importantCount; i++) {
                if (lastMessage[importantLinks[i]] < id) {
                    // The target for this important link has not yet
                    // acknowledged the message
                    return MSG_SEND;
                }
            }
            // All important links have acknowledged the message
        }
        return MSG_CANCEL;
    }

    public BroadcastEvent sendBroadcast(long time, int type, int id) {
        // Hack to get the triggering to make the triggering node to understand
        // that is was sending the first message and this have no acks when it
        if (id > lastSentID) {
            ackCount = 0;
        }

        BroadcastEvent evt = super.sendBroadcast(time, type, id);
        evt.setMaxRetransmissions((isHandlingImportantLinks && importantCount > 0) ? 4 : maxResends);
        return evt;
    }

    public void sendAck(long time, Node dst, int id) {
        // An ack is also a packet... but only for one node...
        if (DEBUG)
            System.out.println(" -- Node: " + nodeID + " sending ack to " + dst.nodeID);
        Link link = getLink(dst);
        if (link != null) {
            sendMessage(ERROR_RATE, link, time + 1, TYPE_ACK, id);
        }
    }

    // -------------------------------------------------------------------
    // Policy handling
    // -------------------------------------------------------------------

    private final static int[] NEIGHBOR_STATE = { 0, 0, 1, 2, 3, 3, 4, 4, 5 };
    private final static int NEIGHBOR_STATE_COUNT = 6;

    public int getState() {
        Node[] neighbors = getNeighbors();
        int neighborCount = neighbors == null ? 0 : neighbors.length;
        if (neighborCount >= NEIGHBOR_STATE.length) {
            neighborCount = NEIGHBOR_STATE.length - 1;
        }
        return getState(neighborCount, importantCount);
    }

    public String getStateAsString(int state) {
        int neighborState = state / 2;
        int importantState = state % 2;
        return "[neighbors=" + neighborState + ",importantLinks=" + importantState + ']';
    }

    private int getState(int neighborCount, int importantCount) {
        return NEIGHBOR_STATE[neighborCount] * 2 + (importantCount > 0 ? 1 : 0);
    }

    public int getStateCount() {
        return NEIGHBOR_STATE_COUNT * 2;
    }

    public int getActionCount() {
        return 4 * 3 * 2;
    }

    public String getActionAsString(int action) {
        StringBuilder sb = new StringBuilder();
        boolean isHandlingImportantLinks = (action & 1) == 1;
        action >>= 1;
        int resendAction = action / 3;
        int thresholdAction = action % 3;
        sb.append('[');
        switch (resendAction) {
        case 0:
            sb.append("maxResends=0");
            break;
        case 1:
            sb.append("maxResends=1");
            break;
        case 2:
            sb.append("maxResends=2");
            break;
        case 3:
            sb.append("maxResends=3");
            break;
        default:
            System.err.println("illegal resend action: " + resendAction);
            break;
        }
        switch (thresholdAction) {
        case 0:
            sb.append(",threshold=0.5");
            break;
        case 1:
            sb.append(",threshold=0.66");
            break;
        case 2:
            sb.append(",threshold=1.0");
            break;
        default:
            System.err.println("illegal threshold action: " + thresholdAction);
            break;
        }
        if (isHandlingImportantLinks) {
            sb.append(",handleImportantLinks");
        }
        return sb.append(']').toString();
    }

    public void performAction(int action) {
        // this.lastAction = action;

        isHandlingImportantLinks = (action & 1) == 1;
        action >>= 1;
        int resendAction = action / 3;
        int thresholdAction = action % 3;
        switch (resendAction) {
        case 0:
            maxResends = 0;
            break;
        case 1:
            maxResends = 1;
            break;
        case 2:
            maxResends = 2;
            break;
        case 3:
            maxResends = 3;
            break;
        default:
            System.err.println("illegal resend action: " + resendAction);
            break;
        }
        switch (thresholdAction) {
        case 0:
            threshold = 0.5;
            break;
        case 1:
            threshold = 0.66;
            break;
        case 2:
            threshold = 1.0;
            break;
        default:
            System.err.println("illegal threshold action: " + thresholdAction);
            break;
        }
        updateVisualizer();
    }

    protected void initPolicy() {
        // if (policy == null) {
        // // Setup policy with instructions for RBP
        // policy = new int[getStateCount()];
        // initPolicyForRBP(policy);
        // qValue = new double[getStateCount()][getActionCount()];
        // qValueSum = new double[getStateCount()][getActionCount()];
        // qCount = new int[getStateCount()][getActionCount()];
        // }
    }

    public void runPolicy() {
        // int state = getState();
        // performAction(policy[state]);
        if (policyManager != null) {
            performAction(policyManager.getPolicyAction(this));
        }
    }

    // public void performExperiment() {
    // int action = (int) (Math.random() * getActionCount());
    // System.out.println("Node " + nodeID + " performed experiment: " +
    // action);
    // performAction(action);
    // }

    public void initPolicyForRBP(int[] policy) {
        for (int i = 0, n = NEIGHBOR_STATE.length; i < n; i++) {
            if (i >= 4 && i <= 7) {
                // threshold = 0.66, maxResends = 2
                policy[getState(i, 0)] = ((2 * 3) + 1) * 2 + 0;
                policy[getState(i, 1)] = ((2 * 3) + 1) * 2 + 1;
            } else if (i >= 8) {
                // threshold = 0.5, maxResends = 1
                policy[getState(i, 0)] = ((1 * 3) + 0) * 2 + 0;
                policy[getState(i, 1)] = ((1 * 3) + 0) * 2 + 1;
            } else {
                // threshold = 1.0, maxResends = 3
                policy[getState(i, 0)] = ((3 * 3) + 2) * 2 + 0;
                policy[getState(i, 1)] = ((3 * 3) + 2) * 2 + 1;
            }
        }
    }

    public void initPolicyForFLD(int[] policy) {
        for (int i = 0, n = policy.length; i < n; i++) {
            // threshold = 0.5, maxResends = 0
            policy[i] = ((0 * 3) + 0) * 2 + 0;
        }
    }

    // -------------------------------------------------------------------
    // Episode stuff...
    // -------------------------------------------------------------------

    public void updateEpisodeStat(EpisodeStat stat) {
        stat.bytesSent += (int) (bytesSent - lastBytes);
        if (floodReceived)
            stat.floodReceived++;
        lastBytes = bytesSent;
        floodReceived = false;
    }

    // Each node learning its own policy, evaluated together....
    // public void updateEpisode(double utility) {
    // int state = getState();
    // int action = lastAction;
    // int count = ++qCount[state][action];
    // qValueSum[state][action] += utility;
    // qValue[state][action] = qValueSum[state][action] / count;
    // }

    // public boolean improvePolicy() {
    // int state = getState();
    // double[] qStateValue = qValue[state];

    // double bestValue = qStateValue[0];
    // int bestAction = 0;
    // for (int i = 1, n = qStateValue.length; i < n; i++) {
    // if (qStateValue[i] > bestValue) {
    // bestValue = qStateValue[i];
    // bestAction = i;
    // }
    // }
    // if (policy[state] != bestAction) {
    // policy[state] = bestAction;
    // return true;
    // }
    // return false;
    // }

} // PolicyNode
