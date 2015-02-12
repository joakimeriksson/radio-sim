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
 * $Id: LineSetup.java,v 1.4 2007/05/31 18:18:12 joakime Exp $
 *
 * -----------------------------------------------------------------
 *
 * LineSetup
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Thu May 24 15:06:32 2007
 * Updated : $Date: 2007/05/31 18:18:12 $
 *           $Revision: 1.4 $
 */
package se.sics.sim.examples;

import se.sics.sim.core.*;
import se.sics.sim.net.*;

/**
 */
public class LineSetup extends AbstractSetup {

    private Node[] nodes;
    private int floods = 0;
    private int debug = 0;
    public static int NUMBER_OF_NODES = 10;

    public void setup(Simulator sim) {
        super.setup(sim);
        Class<?> nodeType = (Class<?>) getProperty("node.class");
        Class<?> macLayerType = (Class<?>) getProperty("macLayer.class");
        Class<?> applicationType = (Class<?>) getProperty("application.class");
        Object policyType = getProperty("policy.instance");

        String numberOfNodes = (String) getProperty("network.numberOfNodes");

        if (nodeType == null) {
            throw new IllegalStateException("no node class specified");
        }
        if (numberOfNodes != null) {
            NUMBER_OF_NODES = Integer.parseInt(numberOfNodes);
        }
        floods = getPropertyAsInt("floods", 100000);

        if (0 < debug) {
            System.out.println("Setting up a " + NUMBER_OF_NODES + " nodes line using node type " + nodeType.getName());
            System.out.println("-----------------------------");
        }
        nodes = PlacementUtil.setupLine(sim, NUMBER_OF_NODES, nodeType, macLayerType, applicationType, policyType);
        PlacementUtil.makeImportantLinks(sim, nodes, nodes[0]);
        if (sim.visualizer != null) {
            sim.visualizer.setNodeProperty(nodes[0].nodeID, "Type", "Sink");
            sim.visualizer.setNodeProperty(nodes[nodes.length - 1].nodeID, "Type", "Outermost Tier");
        }
        if (nodes[0] instanceof MessageNode) {
            sim.addEvent(new BCTrigger(sim, 0, (MessageNode) nodes[0], FLDNode.TYPE_MESSAGE, floods));
        }
    }

    public void finish(Simulator sim) {
        // StatUtil.printRCMStat(nodes, floods);
    }

} // LineSetup
