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
 * $Id: MeshDisjointSetup.java,v 1.3 2007/05/25 12:04:00 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * MeshDisjointSetup
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Thu May 24 15:09:16 2007
 * Updated : $Date: 2007/05/25 12:04:00 $
 *           $Revision: 1.3 $
 */
package se.sics.sim.examples;

import se.sics.sim.core.*;
import se.sics.sim.net.*;
import se.sics.sim.util.*;

/**
 */
public class MeshDisjointSetup extends AbstractSetup {

    private Node[] nodes;
    private int floods = 0;

    public void setup(Simulator sim) {
        Class<?> nodeType = (Class<?>) getProperty("node.class");
        if (nodeType == null) {
            throw new IllegalStateException("no node class specified");
        }
        floods = getPropertyAsInt("floods", 100000);
        MessageNode.ERROR_RATE = 0.11;

        System.out.println("Setting up a 5 level disjoint mesh with node type " + nodeType.getName());
        nodes = PlacementUtil.setupMeshD(sim, 5, nodeType, PlacementUtil.MESH_FWD, 0);
        PlacementUtil.makeImportantLinks(sim, nodes, nodes[0]);
        if (sim.visualizer != null) {
            sim.visualizer.setNodeProperty(nodes[0].nodeID, "Type", "Sink");
            sim.visualizer.setNodeProperty(nodes[nodes.length - 1].nodeID, "Type", "Outermost Tier");
        }
        System.out.println("Node count: " + nodes.length);
        System.out.println("-----------------------------");
        sim.addEvent(new BCTrigger(sim, 0, (MessageNode) nodes[0], MessageNode.TYPE_MESSAGE, floods));
    }

    public void finish(Simulator sim) {
        StatUtil.printRCMStat(nodes, floods);
    }

} // MeshDisjointSetup
