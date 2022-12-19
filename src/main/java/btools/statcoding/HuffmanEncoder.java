package btools.statcoding;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Encoder for huffman-encoding objects
 * <p>
 * It detects identical objects and sorts them
 * into a huffman-tree according to their frequencies
 * <p>
 * Adapted for 2-pass encoding (statistics -&gt; encoding )
 */
public abstract class HuffmanEncoder {

  protected BitOutputStream bos;

  private HashMap<Object, TreeNode> symbols = new HashMap<Object, TreeNode>();
  private int pass;
  private long nextTagValueSetId;

  public void encodeObject(Object obj) throws IOException {
    TreeNode tn = symbols.get(obj);
    if (pass == 2) {
      if ( tn == null ) {
    	  throw new IllegalArgumentException( "symbol was not seen in pass 1: " + obj );
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

  public void init(BitOutputStream bos)  throws IOException {
    this.bos = bos;
    if (++pass == 2) { // encode the dictionary in pass 2
    	
    	boolean hasSymbols = !symbols.isEmpty();
    	bos.encodeBit( hasSymbols );
      if ( hasSymbols ) {
        PriorityQueue<TreeNode> queue = new PriorityQueue<TreeNode>(2 * symbols.size(), new TreeNode.FrequencyComparator());
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
      encodeObjectToStream( node.obj );
    }
  }

  protected abstract void encodeObjectToStream(Object obj) throws IOException;
  
  public String getStats() {
  	double entropy = 0.;
  	long bits = 0L;
  	long totfreq = 0L;
  	int distinct = 0;
    for( TreeNode tn : symbols.values() ) {
    	long r = tn.range;
    	int nbits = 0;
    	while( r > 1L ) {
    	  nbits++;
    	  r >>>= 1;
    	}
    	totfreq += tn.frequency;
    	bits += tn.frequency * nbits;
    	entropy += Math.log( tn.frequency ) * tn.frequency;
    	distinct++;
    }
    entropy = ( Math.log( totfreq ) * totfreq - entropy ) / Math.log( 2 );
    return "symbols=" + totfreq + " distinct=" + distinct + " bits=" + bits; // + " entropy=" + entropy;
  }
  

  private static final class TreeNode {
    Object obj;
    long frequency, code, range;
    TreeNode child1, child2;
    long id; // serial number to make the comparator well defined in case of equal frequencies

    public TreeNode(long id) {
      this.id = id;
    }

    public static class FrequencyComparator implements Comparator<TreeNode> {

      @Override
      public int compare(TreeNode tn1, TreeNode tn2) {
        if (tn1.frequency < tn2.frequency)
          return -1;
        if (tn1.frequency > tn2.frequency)
          return 1;

        // to avoid ordering instability, decide on the id if frequency is equal
        if (tn1.id < tn2.id)
          return -1;
        if (tn1.id > tn2.id)
          return 1;
        return 0;
      }
    }

  }
}
