package btools.statcoding;

/**
 * DataInputStream for decoding fast-compact encoded number sequences
 *
 * @author ab
 */
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


public final class BitInputStream extends DataInputStream {

  private int bits; // bits left in buffer
  private long b; // buffer word

  public BitInputStream(InputStream is) {
    super(is);
  }

  public final boolean decodeBit() throws IOException {
    fillBuffer();
    boolean value = ((b & 1L) != 0L);
    b >>>= 1;
    bits--;
    return value;
  }

  /**
   * @see #encodeVarBits
   */
  public final long decodeVarBits() throws IOException {
    long range = 0L;
    while (!decodeBit()) {
      range = ( range << 1) | 1L;
    }
    return range + decodeBounded(range);
  }

  public final long decodeSignedVarBits() throws IOException {
    boolean isNegative = decodeBit();
    long lv =  decodeVarBits();
    return isNegative ? -lv-1L : lv;
  }

  /**
   * decode an integer in the range 0..max (inclusive).
   *
   * @see #encodeBounded
   */
  public final long decodeBounded(long max) throws IOException {
    long value = 0L;
    long im = 1L; // integer mask
    while ( im > 0 && (value | im) <= max) {
      if (decodeBit()) {
        value |= im;
      }
      im <<= 1;
    }
    return value;
  }

  private void fillBuffer() throws IOException {
    while (bits < 56) {
      int nextByte = read();

      if (nextByte != -1) {
        b |= (nextByte & 0xffL) << bits;
      }
      bits += 8;
    }
  }

}
