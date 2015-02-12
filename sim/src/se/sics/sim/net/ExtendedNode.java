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
 * $Id: ExtendedNode.java,v 1.1 2007/05/25 13:12:36 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * ExtendedNode
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Fri May 25 15:03:45 2007
 * Updated : $Date: 2007/05/25 13:12:36 $
 *           $Revision: 1.1 $
 */
package se.sics.sim.net;

import se.sics.sim.core.*;

/**
 */
public abstract class ExtendedNode extends MessageNode {

    protected int[] importantLinks;
    protected int importantCount = 0;

    protected void init() {
        super.init();

        Link[] neighbors = getLinks();
        int neighborCount = neighbors == null ? 0 : neighbors.length;
        // Cache information about important link for quicker access
        importantLinks = new int[importantCount];
        if (importantCount > 0) {
            int index = 0;
            for (int i = 0; i < neighborCount; i++) {
                if ((neighbors[i] instanceof ExtendedLink) && ((ExtendedLink) neighbors[i]).isImportant()) {
                    if (index == importantCount) {
                        throw new IllegalStateException("Too many important links!");
                    }
                    importantLinks[index++] = i;
                }
            }
            if (index < importantCount) {
                throw new IllegalStateException("Too few important links!");
            }
        }
    }

    protected Link createLinkToNode(String id, Node destination) {
        return new ExtendedLink(id, this, destination);
    }

    public void addImportantLink(Node destination) {
        importantCount++;
        ((ExtendedLink) createLink(destination)).setImportant(true);
    }

    // -------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------

    public static int indexOf(Node[] nodes, Node node) {
        if (nodes != null) {
            for (int i = 0, n = nodes.length; i < n; i++) {
                if (nodes[i] == node) {
                    return i;
                }
            }
        }
        return -1;
    }

} // ExtendedNode
