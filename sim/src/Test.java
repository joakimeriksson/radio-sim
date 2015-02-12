/**
 * Describe class Test here.
 *
 *
 * Created: Thu Apr 19 17:22:02 2007
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class Test {

    /**
     * Creates a new <code>Test</code> instance.
     *
     */
    public Test() {

    }

    public static void main(String[] args) {
        // A test for five nodes in a line sending using FLD
        int simulations = 1000000;
        int nodeCount = 5;
        long start = System.currentTimeMillis();
        Node[] nodes = new Node[nodeCount];
        for (int i = 0, n = nodes.length; i < n; i++) {
            nodes[i] = new Node(i + 1);

            if (i > 1) {
                nodes[i - 1].setNeigbours(new Node[] { nodes[i - 2], nodes[i] });
            }
        }
        nodes[0].setNeigbours(new Node[] { nodes[1] });
        nodes[nodes.length - 1].setNeigbours(new Node[] { nodes[nodes.length - 2] });

        // nodes[nodes.length - 1].endNode = true;

        int success = 0;
        for (int i = 0, n = simulations; i < n; i++) {
            nodes[0].newMsg();
            boolean moretodo = true;
            while (moretodo) {
                moretodo = false;
                for (int j = 0, m = nodes.length; j < m; j++) {
                    moretodo |= nodes[j].tick();
                }
            }
            if (nodes[nodes.length - 1].received == nodes[0].received)
                success++;
        }

        System.out.println("Time Elapsed: " + (System.currentTimeMillis() - start));

        int packetSize = 20;
        int bytesPerFlood = (packetSize * Node.packetCtr) / simulations;
        double ratio = (1.0 * success / simulations);
        // 99% reliability for the flood!
        // 0.01 = (1 - ratio) ^ F
        // log 0.01 = log ((1 - ratio) ^ F) = log(1 - ratio) * F
        // F = log 0.01 / log (1 - ratio)
        double nprFlood = Math.log(0.01) / Math.log(1 - ratio);

        double rcm = (nprFlood * bytesPerFlood) / (nodeCount * packetSize);

        System.out.println("Message succes rate: " + ratio * 100);
        System.out.println("Neccesary floods for 99% => " + nprFlood);
        System.out.println("Packets sent: " + Node.packetCtr);
        System.out.println("bytesPerFlood (10): " + bytesPerFlood);
        System.out.println("RCM: " + rcm);
    }

}
