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
 * $Id: EpisodeStat.java,v 1.3 2007/06/01 11:05:54 joakime Exp $
 *
 * -----------------------------------------------------------------
 *
 * EpisodeStat
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Thu May 31 13:53:13 2007
 * Updated : $Date: 2007/06/01 11:05:54 $
 *           $Revision: 1.3 $
 */
package se.sics.sim.rl;

/**
 */
public class EpisodeStat {

    public int bytesSent = 0;
    public int floodReceived = 0;
    private int episode = 0;
    private int nodeCount = 0;
    private double nodeFac;
    private double byteFac;

    public static final int BYTE_SIZE = 80;

    public EpisodeStat(int initialNodeCount) {
        nodeCount = initialNodeCount;
        nodeFac = 10.0 / (nodeCount - 1);
        byteFac = 10.0 / ((nodeCount - 1) * BYTE_SIZE);
    }

    public void nextEpisode() {
        bytesSent = 0;
        floodReceived = 0;
        episode++;
    }

    public void print() {
        System.out.println("Episode: " + episode + " bytes: " + bytesSent + " nodes received: " + floodReceived
                + " Utility: " + getUtility());
    }

    public int getEpisode() {
        return episode;
    }

    public double getUtility() {
        // 10.0 points for full flood!
        double floodRate = floodReceived * nodeFac;
        // System.out.println("Pos: " + floodRate * floodRate + " neg: " +
        // byteFac * bytesSent);
        return floodRate * floodRate - byteFac * bytesSent;

        // A random utility function...
        // return floodReceived - 0.01 * bytesSent;
    }

} // EpisodeStat
