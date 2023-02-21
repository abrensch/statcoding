package btools.statcoding.huffman;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import btools.statcoding.BitOutputStream;

/**
 * Encoder for huffman-encoding objects.
 * <br><br>
 * It detects identical objects and sorts them into a huffman-tree according to
 * their frequencies.
 * <br><br>
 * Adapted for 2-pass encoding (pass 1: statistic collection, pass 2: encoding).
 */
public abstract class HuffmanEncoder {

    protected BitOutputStream bos;

    private final HashMap<Object, TreeNode> symbols = new HashMap<>();
    private int pass;
    private long nextTagValueSetId;

    public void encodeObject(Object obj) throws IOException {
        TreeNode tn = symbols.get(obj);
        if (pass == 2) {
            if (tn == null) {
                throw new IllegalArgumentException("symbol was not seen in pass 1: " + obj);
            }
            bos.encodeBounded(tn.range - 1, tn.code);
        } else {
            if (tn == null) {
                tn = new TreeNode(nextTagValueSetId++);
                tn.obj = obj;
                symbols.put(obj, tn);
            }
            tn.frequency++;
        }
    }

    public void init(BitOutputStream bos) throws IOException {
        this.bos = bos;
        if (++pass == 2) { // encode the dictionary in pass 2

            boolean hasSymbols = !symbols.isEmpty();
            bos.encodeBit(hasSymbols);
            if (hasSymbols) {
                PriorityQueue<TreeNode> queue = new PriorityQueue<>(2 * symbols.size(),
                        new TreeNode.FrequencyComparator());
                queue.addAll(symbols.values());
                while (queue.size() > 1) {
                    TreeNode node = new TreeNode(nextTagValueSetId++);
                    node.child1 = queue.poll();
                    node.child2 = queue.poll();
                    node.frequency = node.child1.frequency + node.child2.frequency;
                    queue.add(node);
                }
                TreeNode root = queue.poll();
                encodeTree(root, 1, 0);
            }
        }
    }

    public void encodeTree(TreeNode node, long range, long code) throws IOException {
        node.range = range;
        node.code = code;
        boolean isNode = node.child1 != null;
        bos.encodeBit(isNode);
        if (isNode) {
            encodeTree(node.child1, range << 1, code);
            encodeTree(node.child2, range << 1, code + range);
        } else {
            encodeObjectToStream(node.obj);
        }
    }

    protected abstract void encodeObjectToStream(Object obj) throws IOException;

    public String getStats() {
        double entropy = 0.;
        long bits = 0L;
        long totFreq = 0L;
        int distinct = 0;
        for (TreeNode tn : symbols.values()) {
            long r = tn.range;
            int nBits = 0;
            while (r > 1L) {
                nBits++;
                r >>>= 1;
            }
            totFreq += tn.frequency;
            bits += tn.frequency * nBits;
            entropy += Math.log(tn.frequency) * tn.frequency;
            distinct++;
        }
        entropy = (Math.log(totFreq) * totFreq - entropy) / Math.log(2);
        return "symbols=" + totFreq + " distinct=" + distinct + " bits=" + bits + " entropy=" + entropy;
    }

    private static final class TreeNode {
        Object obj;
        long frequency, code, range;
        TreeNode child1, child2;
        long id; // serial id to make the comparator well-defined for equal frequencies

        public TreeNode(long id) {
            this.id = id;
        }

        public static class FrequencyComparator implements Comparator<TreeNode> {

            @Override
            public int compare(TreeNode tn1, TreeNode tn2) {
                // to avoid ordering instability, decide on the id if frequency is equal
                int result = Long.compare( tn1.frequency, tn2.frequency );
                return result != 0 ? result : Long.compare( tn1.id, tn2.id );
            }
        }

    }
}
