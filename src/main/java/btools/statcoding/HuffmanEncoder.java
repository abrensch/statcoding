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

  private HashMap<Object, TreeNode> identityMap = new HashMap<Object, TreeNode>();
  private int pass;
  private int nextTagValueSetId;

  public void encodeObject(Object obj) throws IOException {
    TreeNode tn = identityMap.get(obj);
    if (pass == 2) {
      bos.encodeBounded(tn.range - 1, tn.code);
    } else {
      if (tn == null) {
        tn = new TreeNode(nextTagValueSetId++);
        tn.obj = obj;
        identityMap.put(obj, tn);
      }
      tn.frequency++;
    }
  }

  public void init(BitOutputStream bos)  throws IOException {
    this.bos = bos;
    if (++pass == 2) { // encode the dictionary in pass 2
      if (identityMap.size() == 0) {
        TreeNode dummy = new TreeNode(nextTagValueSetId++);
        identityMap.put(dummy, dummy);
      }
      PriorityQueue<TreeNode> queue = new PriorityQueue<TreeNode>(2 * identityMap.size(), new TreeNode.FrequencyComparator());
      queue.addAll(identityMap.values());
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

  public void encodeTree(TreeNode node, int range, int code) throws IOException {
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

  private static final class TreeNode {
    public Object obj;
    public int frequency;
    public int code;
    public int range;
    public TreeNode child1;
    public TreeNode child2;
    private int id; // serial number to make the comparator well defined in case of equal frequencies

    public TreeNode(int id) {
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

        if (tn1 != tn2) {
          throw new RuntimeException("identity corruption!");
        }
        return 0;
      }
    }

  }
}
