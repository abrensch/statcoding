package btools.statcoding;

/**
 * DataOutputStream for fast-compact encoding of number sequences
 *
 * @author ab
 */
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public final class BitOutputStream extends DataOutputStream {

  private int bits; // bits left in buffer
  private long b; // buffer word

  public BitOutputStream(OutputStream os) {
    super(os);
  }

  private void flushBuffer() throws IOException {
    while (bits > 7) {
      writeByte( (byte) (b & 0xffL) );
      b >>>= 8;
      bits -= 8;
    }
  }

  @Override
  public void close() throws IOException {
    flushBuffer();
    if (bits > 0) {
      writeByte( (byte) (b & 0xff) );
    }
  	super.close();
  }


  public final void encodeBit(boolean value) throws IOException {
    flushBuffer();
    if (value) {
      b |= 1L << bits;
    }
    bits++;
  }

  public final void encodeVarBits(long value) throws IOException {
    if ( value < 0 ) {
      throw new IllegalArgumentException( "encodeVarBits expects positive values: " + value );
    }
    long range = 0L;
    while (value > range) {
      encodeBit(false);
      value -= range + 1L;
      range = (range << 1) | 1L;
    }
    encodeBit(true);
    encodeBounded(range, value);
  }

  public final void encodeSignedVarBits(long value) throws IOException {
    encodeBit( value < 0L );
    encodeVarBits( value < 0L ? -value-1L : value );
  }

  /**
   * encode a long in the range 0..max (inclusive).
   * For max = 2^n-1, this just encodes n bits, but in general
   * this is variable length encoding, with the shorter codes
   * for the central value range
   */
  public final void encodeBounded(long max, long value) throws IOException {  
    if ( max < 0L || value < 0 ) {
      throw new IllegalArgumentException( "encodeBounded expects positive values" );
    }
    long im = 1L; // integer mask
    while (im > 0 && im <= max) {
      if ((value & im) != 0L) {
        encodeBit(true);
        max -= im;
      } else {
        encodeBit(false);
      }
      im <<= 1;
    }
  }
}
