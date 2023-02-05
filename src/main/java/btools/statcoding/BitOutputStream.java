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
  private long bytesWritten = 0L;

  public BitOutputStream(OutputStream os) {
    super(os);
  }

  private void flushBuffer() throws IOException {
    while (bits > 7) {
      writeByte( (byte) (b & 0xffL) );
      b >>>= 8;
      bits -= 8;
      bytesWritten++;
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
  
  public long getBitPosition() {
  	return bytesWritten * 8L + bits;
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

  /**
   * encode an unsigned integer with some of of least significant bits
   * considered noisy
   *
   * @see #decodeNoisyNumber
   */
  public void encodeNoisyNumber(long value, int noisybits) throws IOException {
    if (value < 0L) {
      throw new IllegalArgumentException("encodeNoisyNumber expects positive value");
    }
    if (noisybits > 0) {
      long mask = 0xffffffffffffffffL >>> (64 - noisybits);
      encodeBounded(mask, value & mask);
      value >>= noisybits;
    }
    encodeVarBits(value);
  }

  public void encodeSortedArray(long[] values) throws IOException {
    int size = values.length;
    encodeVarBits( size );
    encodeSortedArray( values, size, 0 );
  }

  public void encodeSortedArray(long[] values, int size, int minLengthBits ) throws IOException {
    if ( size > 0 ) {
      long max = values[size-1];
      int nbits = 0;
      while ( (max >>>= 1) != 0L ) {
    	nbits++;
      }
      encodeNoisyNumber( nbits, minLengthBits );
      encodeSortedArray( values, 0, size, 1L << nbits, 0L );
    }
  }

  /**
   * encode an integer-array making use of the fact that it is sorted. This is
   * done, starting with the most significant bit, by recursively encoding the
   * number of values with the current bit being 0. This yields an number of
   * bits per value that only depends on the typical distance between subsequent
   * values and also benefits
   *
   * @param values  the array to encode
   * @param offset  position in this array where to start
   * @param subsize number of values to encode
   * @param nextbit bitmask with the most significant bit set to 1
   * @param mask    should be 0
   */
  public void encodeSortedArray(long[] values, int offset, int subsize, long nextbit, long mask) throws IOException {
    if (subsize == 1) // last-choice shortcut
    {
      while (nextbit != 0L) {
        encodeBit((values[offset] & nextbit) != 0L);
        nextbit >>>= 1;
      }
    }
    if (nextbit == 0L) {
      return;
    }

    long data = mask & values[offset];
    mask |= nextbit;

    // count 0-bit-fraction
    int i = offset;
    int end = subsize + offset;
    for (; i < end; i++) {
      if ((values[i] & mask) != data) {
        break;
      }
    }
    int size1 = i - offset;
    int size2 = subsize - size1;

    if ( subsize > nextbit ) {
      long max = nextbit;
      long min = subsize-nextbit;
      encodeBounded(max-min, size1-min);
    } else {
      encodeBounded(subsize, size1);
    }
    if (size1 > 0) {
      encodeSortedArray(values, offset, size1, nextbit >>> 1, mask);
    }
    if (size2 > 0) {
      encodeSortedArray(values, i, size2, nextbit >>> 1, mask);
    }
  }
}
