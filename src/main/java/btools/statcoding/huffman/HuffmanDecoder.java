package btools.statcoding.huffman;

import java.io.IOException;

import btools.statcoding.BitInputStream;

/**
 * Decoder for huffman-encoded objects.
 * <br><br>
 * Uses a lookup-table of configurable size to speed up decoding.
 * <br><br>
 * This is an abstract class because the method decodeObjectFromStream must be
 * implemented to decode the leafs of the huffman tree from the input stream.
 */
public abstract class HuffmanDecoder {

    protected BitInputStream bis;

    private int lookupBits;
    private Object[] subtrees;
    private int[] lengths;

    public final Object decodeObject() throws IOException {
        int idx = bis.decodeLookupIndex(lengths);
        Object node = subtrees[idx];
        while (node instanceof TreeNode) {
            TreeNode tn = (TreeNode) node;
            boolean nextBit = bis.decodeBit();
            node = nextBit ? tn.child2 : tn.child1;
        }
        return node;
    }

    /**
     * See {@link #init(BitInputStream, int)}
     *
     * This initializes with the default lookup sizing (8 bit = 256 entries).
     *
     * @param bis the input stream to decode the tree and the symbols from
     */
    public void init(BitInputStream bis) throws IOException {
        init(bis, 8);
    }

    /**
     * Initialize this huffman decoder. That decodes the tree from the underlying
     * input stream and builds a lookup table of the given size.
     *
     * @param bis        the input stream to decode the tree and the symbols from
     * @param lookupBits use a lookup table of size 2^lookupBits for speedup
     */
    public void init(BitInputStream bis, int lookupBits) throws IOException {

        if (lookupBits < 0 || lookupBits > 20) {
            throw new IllegalArgumentException("lookupBits ot of range ( 0..20 ): " + lookupBits);
        }
        this.bis = bis;
        this.lookupBits = lookupBits;
        boolean hasSymbols = bis.decodeBit();
        if (hasSymbols) {
            subtrees = new Object[1 << lookupBits];
            lengths = new int[1 << lookupBits];
            decodeTree(0, 0);
        }
    }

    protected abstract Object decodeObjectFromStream() throws IOException;

    private Object decodeTree(int offset, int bits) throws IOException {
        boolean isNode = bis.decodeBit();
        int step = bits <= lookupBits ? 1 << bits : 0;
        if (isNode) {
            Object child1 = decodeTree(offset, bits + 1);
            Object child2 = decodeTree(offset + step, bits + 1);
            if (bits < lookupBits) {
                return null;
            }
            TreeNode node = new TreeNode();
            node.child1 = child1;
            node.child2 = child2;
            if (bits == lookupBits) {
                subtrees[offset] = node;
                lengths[offset] = bits;
            }
            return node;
        }
        Object leaf = decodeObjectFromStream();
        if (step > 0) {
            for (int i = offset; i < subtrees.length; i += step) {
                subtrees[i] = leaf;
                lengths[i] = bits;
            }
        }
        return leaf;
    }

    private static final class TreeNode {
        public Object child1;
        public Object child2;
    }
}
