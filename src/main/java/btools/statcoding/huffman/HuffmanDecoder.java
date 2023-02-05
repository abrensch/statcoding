package btools.statcoding.huffman;

import java.io.IOException;

import btools.statcoding.BitInputStream;

/**
 * Decoder for huffman-encoded objects
 */
public abstract class HuffmanDecoder {

  protected BitInputStream bis;

  private Object tree;

  public Object decodeObject() throws IOException {
    Object node = tree;
    while (node instanceof TreeNode) {
      TreeNode tn = (TreeNode) node;
      boolean nextBit = bis.decodeBit();
      node = nextBit ? tn.child2 : tn.child1;
    }
    return node;
  }

  public void init(BitInputStream bis) throws IOException {
    this.bis = bis;
    boolean hasSymbols = bis.decodeBit();
    if ( hasSymbols ) {
      tree = decodeTree();
    }
  }
  
  protected abstract Object decodeObjectFromStream() throws IOException ;

  private Object decodeTree() throws IOException {
    boolean isNode = bis.decodeBit();
    if (isNode) {
      TreeNode node = new TreeNode();
      node.child1 = decodeTree();
      node.child2 = decodeTree();
      return node;
    }
    return decodeObjectFromStream();
  }

  private static final class TreeNode {
    public Object child1;
    public Object child2;
  }
}
