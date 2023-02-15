package btools.statcoding;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * BitInputStream is a replacement for java.io.DataInputStream extending it by
 * bitwise operations suitable for statistical decoding.
 *
 * It automatically re-aligns to byte-alignment as soon as any of the methods of
 * InputStream or DataOutput or its own method 'decodeVarBytes' is called.
 * 
 * Please note that BitInputStream buffers up to 8 bytes from the underlying
 * stream, and it has a somewhat sloppy EOF detection, so it may not work as a
 * plugin-replacement for java.io.DataInputStream in all cases.
 */
public final class BitInputStream extends InputStream implements DataInput {

    private int bits; // bits left in buffer
    private int eofBits; // dummy bits read after eof
    private long b; // buffer word

    private InputStream in;
    private DataInputStream dis; // created lazily if needed

    public BitInputStream(InputStream is) {
        in = is;
    }

    private int readInternal() throws IOException {
        return in.read();
    }

    private void fillBuffer() throws IOException {
        while (bits <= 56) {
            int nextByte = readInternal();

            if (nextByte != -1) {
                b |= (nextByte & 0xffL) << bits;
            } else {
                eofBits += 8;
            }
            bits += 8;
        }
        if (eofBits >= 256) {
            throw new RuntimeException("end of stream !");
        }
    }

    // ******************************************
    // **** METHODS of java.util.InputStream ****
    // ******************************************

    @Override
    public int read() throws IOException {

        if (bits > 0) {
            while ((bits & 7) > 0) { // any padding bits left?
                if ((b & 1L) != 0L) {
                    throw new IOException("re-alignmet-failure: found non-zero padding bit");
                }
                b >>>= 1;
                bits--;
            }
            if (bits > 7) { // can read byte from bit-buffer
                long value = b & 0xffL;
                b >>>= 8;
                bits -= 8;
                return (int) value;
            }
        }
        return readInternal();
    }

    @Override
    public int available() throws IOException {
        return (bits >> 3) + in.available();
    }

    // ****************************************
    // **** METHODS of java.util.DataInput ****
    // ****************************************

    // delegate Methods of DataInput to an instance of
    // DataInputStream created lazily
    private DataInputStream getDis() {
        if (dis == null) {
            dis = new DataInputStream(this);
        }
        return dis;
    }

    @Override
    public void readFully(byte b[]) throws IOException {
        getDis().readFully(b);
    }

    @Override
    public void readFully(byte b[], int off, int len) throws IOException {
        getDis().readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return getDis().skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return getDis().readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return getDis().readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return getDis().readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return getDis().readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return getDis().readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return getDis().readChar();
    }

    @Override
    public int readInt() throws IOException {
        return getDis().readInt();
    }

    @Override
    public long readLong() throws IOException {
        return getDis().readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return getDis().readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return getDis().readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return getDis().readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return getDis().readUTF();
    }

    // ***********************************************
    // **** Byte-aligned Variable Length Encoding ****
    // ***********************************************

    public final long decodeVarBytes() throws IOException {
        long v = 0L;
        for (int shift = 0; shift < 64; shift += 7) {
            int nextByte = read();
            if (nextByte == -1) {
                throw new IOException("unexpected EOF in decodeVarBytes");
            }
            v |= (nextByte & 0x7fL) << shift;
            if ((nextByte & 0x80) == 0) {
                break;
            }
        }
        return restoreSignBit(v);
    }

    private final long restoreSignBit(long value) {
        return (value & 1L) == 0L ? value >>> 1 : -(value >>> 1) - 1L;
    }

    // ***************************************
    // **** Bitwise Fixed Length Encoding ****
    // ***************************************

    public final boolean decodeBit() throws IOException {
        fillBuffer();
        boolean value = ((b & 1L) != 0L);
        b >>>= 1;
        bits--;
        return value;
    }

    public final long decodeBits(int count) throws IOException {
        if (count == 0) {
            return 0L;
        }
        fillBuffer();
        long mask = 0xffffffffffffffffL >>> (64 - count);
        if ( count > bits ) {
          return decodeBounded( mask ); // buffer too small, slow fallback
        }
        long value = b & mask;
        b >>>= count;
        bits -= count;
        return value;
    }

    // ******************************************
    // **** Bitwise Variable Length Encoding ****
    // ******************************************

    /**
     * @see #encodeVarBits
     */
    public final long decodeUnsignedVarBits(int noisybits) throws IOException {
        long noisyValue = decodeBits(noisybits);
        long range = 0L;
        int bits = 0;
        while (!decodeBit()) {
            if (range == -1L) {
                throw new RuntimeException("range overflow");
            }
            range = (range << 1) | 1L;
            bits++;
        }
        return noisyValue | ((range + decodeBits(bits)) << noisybits);
    }

    public final long decodeSignedVarBits(int noisybits) throws IOException {
        boolean isNegative = decodeBit();
        long lv = decodeUnsignedVarBits(noisybits);
        return isNegative ? -lv - 1L : lv;
    }

    /**
     * decode an integer in the range 0..max (inclusive).
     *
     * @see #encodeBounded
     */
    public final long decodeBounded(long max) throws IOException {
        long value = 0L;
        long im = 1L; // integer mask
        while (im > 0 && (value | im) <= max) {
            if (decodeBit()) {
                value |= im;
            }
            im <<= 1;
        }
        return value;
    }

    /**
     * Decoding twin to {@link BitOutputStream#encodeUniqueSortedArray( long[] )}
     *
     * @return the decoded array of sorted, positive, unique longs
     */
    public long[] decodeUniqueSortedArray() throws IOException {
        int size = (int) decodeUnsignedVarBits(0);
        long[] values = new long[size];
        decodeUniqueSortedArray(values, 0, size, 0);
        return values;
    }

    /**
     * Decoding twin to
     * {@link BitOutputStream#encodeUniqueSortedArray( long[], int, int, int )} <br>
     * See also {@link #decodeUniqueSortedArray()}
     *
     * @param values        the array to decode into
     * @param offset        position in this array where to start
     * @param size          number of values to decode
     * @param minLengthBits noisyBits used to encode bitlength of largest value
     */
    public void decodeUniqueSortedArray(long[] values, int offset, int size, int minLengthBits) throws IOException {
        if (size > 0) {
            int nbits = (int) decodeUnsignedVarBits(minLengthBits);
            decodeUniqueSortedArray(values, 0, size, nbits, 0L);
        }
    }

    /**
     * Decoding twin to
     * {@link BitOutputStream#encodeUniqueSortedArray( long[], int, int, long, long )}
     * <br>
     * See also {@link #decodeUniqueSortedArray( long[], int, int, int )}
     *
     * @param values     the array to encode
     * @param offset     position in this array where to start
     * @param subsize    number of values to encode
     * @param nextbitpos bitposition of the most significant bit
     * @param value      should be 0 at recursion start
     */
    protected void decodeUniqueSortedArray(long[] values, int offset, int subsize, int nextbitpos, long value)
            throws IOException {
        if (subsize == 1) // last-choice shortcut
        {
            // ugly here: inverse bit-order then without the last-choice shortcut
            // but we do it that way for performance
            values[offset] = value | decodeBits( nextbitpos + 1 );
            return;
        }
        if (nextbitpos < 0L) {
            while (subsize-- > 0) {
                values[offset++] = value;
            }
            return;
        }

        long nextbit = 1L << nextbitpos;
        int size1;
        if (subsize > nextbit) {
            long max = nextbit;
            long min = subsize - nextbit;
            size1 = (int) (decodeBounded(max - min) + min);
        } else {
            size1 = (int) decodeBounded(subsize);
        }
        int size2 = subsize - size1;

        if (size1 > 0) {
            decodeUniqueSortedArray(values, offset, size1, nextbitpos - 1, value);
        }
        if (size2 > 0) {
            decodeUniqueSortedArray(values, offset + size1, size2, nextbitpos - 1, value | nextbit );
        }
    }
}
