package btools.statcoding;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * BitInputStream is a replacement for java.io.DataInputStream extending it by
 * bitwise operations suitable for statistical decoding.
 * <br>
 * It automatically re-aligns to byte-alignment as soon as any of the methods of
 * InputStream or DataOutput or its own method 'decodeVarBytes' is called.
 * <br>
 * Please note that while doing bitwise operations, BitInputStream buffers up to
 * 8 bytes from the underlying stream and has a somewhat sloppy EOF detection,
 * so please do fully re-align (see {@link BitOutputStream#writeSyncBlock(long)})
 * after bitwise operations for compliant EOF behavior.
 */
public class BitInputStream extends InputStream implements DataInput {

    private int bits; // bits left in buffer
    private int eofBits; // dummy bits read after eof
    private long b; // buffer word

    private final InputStream in;
    private DataInputStream dis; // created lazily if needed

    public BitInputStream(InputStream is) {
        in = is;
    }

    private void fillBuffer() throws IOException {
        while (bits <= 56) {
            int nextByte = in.read();

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

    /**
     * This actually just calls readLong(), but is a method on it's for
     * documentation: if the underlying input stream is still used by other
     * consumers after this BitInputStream is discarded or paused, we need to make
     * sure that it's internal 64-bit buffer is empty. Any block of >=8 bytes of
     * byte-aligned data will do, just make sure that the encoder and the decoder
     * agree on a common structure. <br>
     * <br>
     * See also {@link BitOutputStream#writeSyncBlock( long )} <br>
     *
     * @return the sync block as a long value
     */
    public long readSyncBlock() throws IOException {
        return readLong();
    }

    // ****************************************
    // **** METHODS of java.io.InputStream ****
    // ****************************************

    @Override
    public int read() throws IOException {

        if (bits > 0) {
            reAlign();
            if (bits > 7) { // can read byte from bit-buffer
                long value = b & 0xffL;
                b >>>= 8;
                bits -= 8;
                return (int) value;
            }
        }
        return in.read();
    }

    private void reAlign() throws IOException {
        while ((bits & 7) > 0) { // any padding bits left?
            if ((b & 1L) != 0L) {
                throw new IOException("re-alignment-failure: found non-zero padding bit");
            }
            b >>>= 1;
            bits--;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        reAlign();
        if (len == 0) {
            return 0;
        }
        int lenFromBuffer = 0;
        while (bits > 0 && len > 0) {
            b[off++] = (byte) read();
            len--;
            lenFromBuffer++;
        }
        if (lenFromBuffer > 0) {
            if (len == 0 || available() == 0) {
                return lenFromBuffer;
            }
            int result = in.read(b, off, len);
            return result == -1 ? lenFromBuffer : lenFromBuffer + result;
        }
        return in.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return (bits >> 3) + in.available();
    }

    // **************************************
    // **** METHODS of java.io.DataInput ****
    // **************************************

    // delegate Methods of DataInput to an instance of
    // DataInputStream created lazily
    private DataInputStream getDis() {
        if (dis == null) {
            dis = new DataInputStream(this);
        }
        return dis;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        getDis().readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
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
    // **** Byte-aligned Variable Length Decoding ****
    // ***********************************************

    /**
     * Decoding twin to {@link BitOutputStream#encodeVarBytes( long )}
     *
     * @return the decoded long value
     */
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

    private long restoreSignBit(long value) {
        return (value & 1L) == 0L ? value >>> 1 : -(value >>> 1) - 1L;
    }

    // ***************************************
    // **** Bitwise Fixed Length Encoding ****
    // ***************************************

    /**
     * Decode a single bit.
     *
     * @return true/false for 1/0
     */
    public final boolean decodeBit() throws IOException {
        fillBuffer();
        boolean value = ((b & 1L) != 0L);
        b >>>= 1;
        bits--;
        return value;
    }

    /**
     * Decode a given number of bits.
     *
     * @param count the number of bit to decode
     * @return the decoded value
     */
    public final long decodeBits(int count) throws IOException {
        if (count == 0) {
            return 0L;
        }
        fillBuffer();
        long mask = 0xffffffffffffffffL >>> (64 - count);
        if (count > bits) {
            return decodeBounded(mask); // buffer too small, slow fallback
        }
        long value = b & mask;
        b >>>= count;
        bits -= count;
        return value;
    }

    // ******************************************
    // **** Bitwise Variable Length Decoding ****
    // ******************************************

    /**
     * Decoding twin to
     * {@link BitOutputStream#encodeUnsignedVarBits( long, int )}<br>
     * <br>
     *
     * Please note that {@code noisyBits} must match the value used for encoding.
     *
     * @param noisyBits the number of lower bits considered noisy
     * @return the decoded value
     */
    public final long decodeUnsignedVarBits(int noisyBits) throws IOException {
        long noisyValue = decodeBits(noisyBits);
        long range = 0L;
        int bits = 0;
        while (!decodeBit()) {
            if (range == -1L) {
                throw new RuntimeException("range overflow");
            }
            range = (range << 1) | 1L;
            bits++;
        }
        return noisyValue | ((range + decodeBits(bits)) << noisyBits);
    }

    /**
     * Decoding twin to {@link BitOutputStream#encodeSignedVarBits( long, int )}<br>
     * <br>
     *
     * Please note that {@code noisyBits} must match the value used for encoding.
     *
     * @param noisyBits the number of lower bits considered noisy
     * @return the decoded value
     */
    public final long decodeSignedVarBits(int noisyBits) throws IOException {
        boolean isNegative = decodeBit();
        long lv = decodeUnsignedVarBits(noisyBits);
        return isNegative ? -lv - 1L : lv;
    }

    /**
     * Decoding twin to {@link BitOutputStream#encodeBounded( long, long )}<br>
     * <br>
     *
     * Please note that {@code max} must match the value used for encoding.
     *
     * @param max the number of lower bits considered noisy
     * @return the decoded value
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
     * @param minLengthBits noisyBits used to encode bit-length of largest value
     */
    public void decodeUniqueSortedArray(long[] values, int offset, int size, int minLengthBits) throws IOException {
        if (size > 0) {
            int nBits = (int) decodeUnsignedVarBits(minLengthBits);
            decodeUniqueSortedArray(values, offset, size, nBits, 0L);
        }
    }

    /**
     * Decoding twin to
     * {@link BitOutputStream#encodeUniqueSortedArray( long[], int, int, int, long )}
     * <br>
     * See also {@link #decodeUniqueSortedArray( long[], int, int, int )}
     *
     * @param values     the array to encode
     * @param offset     position in this array where to start
     * @param subSize    number of values to encode
     * @param nextBitPos bit-position of the most significant bit
     * @param value      should be 0 at recursion start
     */
    protected void decodeUniqueSortedArray(long[] values, int offset, int subSize, int nextBitPos, long value)
            throws IOException {
        if (subSize == 1) // last-choice shortcut
        {
            // ugly here: inverse bit-order then without the last-choice shortcut,
            // but we do it that way for performance
            values[offset] = value | decodeBits(nextBitPos + 1);
            return;
        }
        if (nextBitPos < 0L) { // cannot happen for unique, keep code for later
            // while (subSize-- > 0) {
            // values[offset++] = value;
            // }
            // return;
            throw new RuntimeException("unique violation");
        }

        long nextBit = 1L << nextBitPos;
        int size1;
        if (subSize > nextBit) {
            long min = subSize - nextBit;
            size1 = (int) (decodeBounded(nextBit - min) + min);
        } else {
            size1 = (int) decodeBounded(subSize);
        }
        int size2 = subSize - size1;

        if (size1 > 0) {
            decodeUniqueSortedArray(values, offset, size1, nextBitPos - 1, value);
        }
        if (size2 > 0) {
            decodeUniqueSortedArray(values, offset + size1, size2, nextBitPos - 1, value | nextBit);
        }
    }

    /**
     * Decode some bits according to the given lengthArray (which is expected to be
     * 2^n in size, with n <= 32)
     * <br>
     * This is very special logic for speeding up huffman decoding based on a lookup
     * table.
     * <br>
     * But could be used for speeding up other var-length codes as well.
     *
     * @param lengthArray an array telling how much bits to consume for the observed
     *                    bit-pattern
     *
     * @return an index to the lookup array
     */
    public final int decodeLookupIndex(int[] lengthArray) throws IOException {
        fillBuffer();
        int v = (int) (b & (lengthArray.length - 1));
        int count = lengthArray[v];
        b >>>= count;
        bits -= count;
        return v;
    }
}
