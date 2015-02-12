package se.sics.sim.util;

import se.sics.sim.core.Node;
import se.sics.sim.net.*;

/**
 * Describe class StatUtil here.
 *
 *
 * Created: Fri Apr 27 11:10:09 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class StatUtil {

    private StatUtil() {
        // Prevent instances of this class
    }

    public static void printRCMStat(Node[] nodes, int floods) {
        long totalBytes = 0;
        for (int i = 0, n = nodes.length; i < n; i++) {
            totalBytes += nodes[i].bytesSent;
        }
        // Only measure on the last node - that needs to be the sink node!
        double ratio = (1.0 * nodes[nodes.length - 1].msgRecv) / floods;
        // 99% reliability for the flood!
        // 0.01 = (1 - ratio) ^ F
        // log 0.01 = log ((1 - ratio) ^ F) = log(1 - ratio) * F
        // F = log 0.01 / log (1 - ratio)
        double bytesPerFlood = (1.0 * totalBytes) / floods;
        double nprFlood = Math.log(0.01) / Math.log(1 - ratio);
        // if (nprFlood < 1.0) nprFlood = 1.0;
        double rcm = (nprFlood * bytesPerFlood) / (nodes.length * MessageNode.MESSAGE_SIZE[MessageNode.TYPE_MESSAGE]);

        System.out.println("----------- simulation results ------------");
        System.out.println("Total bytes per flood: " + bytesPerFlood);
        System.out.println("Total bytes per perfect flood: " + MessageNode.MESSAGE_SIZE[MessageNode.TYPE_MESSAGE]
                * nodes.length);
        System.out.println("Message success rate: " + ratio * 100);
        System.out.println("Necessary floods for 99% => " + nprFlood);
        System.out.println("RCM: " + rcm);
    }

}
