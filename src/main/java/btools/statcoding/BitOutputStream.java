package btools.statcoding;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * BitOutputStream is a replacement for java.io.DataOutputStream extending it by
 * bitwise operations suitable for statistical encoding.
 * <br>
 * It automatically re-aligns to byte-alignment as soon as any of the methods of
 * OutputStream or DataOutput or its own method 'encodeVarBytes' is called.
 */
public class BitOutputStream extends OutputStream implements DataOutput {

    private int bits; // bits left in buffer
    private long b; // buffer word
    private long bytesWritten;

    private final OutputStream out;
    private DataOutputStream dos; // created lazily if needed

    /**
     * Construct a BitOutputStream for the underlying OutputStream.
     * <br>
     * Please note that BitOutputStream needs exclusive access to the underlying
     * OutputStream because it is buffering bits that could otherwise come out of
     * order.
     */
    public BitOutputStream(OutputStream os) {
        out = os;
    }

    private void writeLowByte(long b) throws IOException {
        writeInternal((int) (b & 0xffL));
    }

    private void writeInternal(int b) throws IOException {
        out.write(b);
        bytesWritten++;
    }

    private void flushBuffer() throws IOException {
        while (bits > 7) {
            writeLowByte(b);
            b >>>= 8;
            bits -= 8;
        }
    }

    private void flushBufferAndReAlign() throws IOException {
        while (bits > 0) {
            writeLowByte(b);
            b >>>= 8;
            bits -= 8;
        }
        bits = 0;
    }

    /**
     * This actually just calls writeLong(), but is a method on it's for
     * documentation: if the underlying output stream is still used by other writers
     * after this BitOutputStream is discarded or paused, we need to make sure that
     * it's internal 64-bit buffer is empty. Any block of >=8 bytes of byte-aligned
     * data will do, just make sure that the encoder and the decoder agree on a
     * common structure. <br>
     * <br>
     * See also {@link BitInputStream#readSyncBlock()} <br>
     *
     * @param value the long value to write as a sync block
     */
    public void writeSyncBlock(long value) throws IOException {
        writeLong(value);
    }

    /**
     * Get the number of bits written so far.
     * <br>
     * This includes padding bits from re-alignment.
     *
     * @return the number of bits.
     */
    public long getBitPosition() {
        return bytesWritten * 8L + bits;
    }

    // *****************************************
    // **** METHODS of java.io.OutputStream ****
    // *****************************************

    @Override
    public void write(int b) throws IOException {
        flushBufferAndReAlign();
        out.write(b);
        bytesWritten++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flushBufferAndReAlign();
        out.write(b, off, len);
        bytesWritten += len;
    }

    /**
     * Flushes the underlying output stream.
     * <br>
     * Please note that this does not trigger re-alignment, so if this
     * BitOutputStream is not currently byte-aligned, then the bit-buffer is not
     * flushed.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        flushBufferAndReAlign();
        out.close();
    }

    // ***************************************
    // **** METHODS of java.io.DataOutput ****
    // ***************************************

    // delegate Methods of DataOutput to an instance of
    // DataOutputStream created lazily
    private DataOutputStream getDos() {
        if (dos == null) {
            dos = new DataOutputStream(this);
        }
        return dos;
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        getDos().writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        getDos().writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        getDos().writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        getDos().writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        getDos().writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        getDos().writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        getDos().writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        getDos().writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        getDos().writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        getDos().writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        getDos().writeUTF(s);
    }

    // ***********************************************
    // **** Byte-aligned Variable Length Encoding ****
    // ***********************************************

    /**
     * Byte aligned variable length encoding of (signed) numbers. This encodes in
     * packages of 7 bits with the 8th bit being the stop-bit.
     * <br><br>
     * The sign bit is put in front so that also negative numbers are encoded
     * effectively. Therefore, the number range that is encoded in a single byte is
     * [-64..63]
     * <br><br>
     * No unsigned flavor of this method is provided. When hunting for space please
     * consider using bitwise encoding.
     *
     * @param value the long value to encode
     *
     * @see BitInputStream#decodeVarBytes
     */
    public final void encodeVarBytes(long value) throws IOException {
        flushBufferAndReAlign();
        long v = moveSignBit(value);
        for (;;) {
            long v7 = v & 0x7f;
            if ((v >>>= 7) == 0L) {
                writeLowByte(v7);
                return;
            }
            writeLowByte(v7 | 0x80L);
        }
    }

    // re-arrange the bits of a signed long to make it better suited for var-length
    // coding
    private long moveSignBit(long value) {
        return value < 0L ? 1L | ((-value - 1L) << 1) | 1 : value << 1;
    }

    // ***************************************
    // **** Bitwise Fixed Length Encoding ****
    // ***************************************

    /**
     * Encode a single bit.
     *
     * @param value the bit to encode
     */
    public final void encodeBit(boolean value) throws IOException {
        flushBuffer();
        if (value) {
            b |= 1L << bits;
        }
        bits++;
    }

    /**
     * Encode a given number of bits.
     *
     * @param nBits the number of bit to encode
     * @param value the value from whom to encode the lower {@code nBits} bits
     */
    public final void encodeBits(int nBits, long value) throws IOException {
        if (nBits > 0 && bits + nBits <= 64) {
            flushBuffer();
            long mask = 0xffffffffffffffffL >>> (64 - nBits);
            b |= (value & mask) << bits;
            bits += nBits;
            return;
        }
        if (nBits < 0 || nBits > 64) {
            throw new IllegalArgumentException("encodeBits: nBits out of range (0..64): " + nBits);
        }
        for (int i = 0; i < nBits; i++) { // buffer too small, slow fallback
            encodeBit((value & (1L << i)) != 0L);
        }
    }

    // ******************************************
    // **** Bitwise Variable Length Encoding ****
    // ******************************************

    /**
     * Bitwise variable length encoding of a non-negative number. The lower
     * {@code noisyBits} bits are encoded directly (see
     * {@link #encodeBits( int, long )}) and for the remaining value the following
     * mapping is used:<br>
     * <br>
     *
     * {@code 1 -> 0}<br>
     * {@code 01 -> 1} + following 1-bit word ( 1..2 )<br>
     * {@code 001 -> 3} + following 2-bit word ( 3..6 )<br>
     * {@code 0001 -> 7} + following 3-bit word ( 7..14 )<br>
     * etc.
     *
     * @param value     the (non-negative) number to encode
     * @param noisyBits the number of lower bits considered noisy
     *
     * @see BitInputStream#decodeUnsignedVarBits(int)
     */
    public final void encodeUnsignedVarBits(long value, int noisyBits) throws IOException {
        if (value < 0) {
            throw new IllegalArgumentException("encodeUnsignedVarBits expects non-negative values: " + value);
        }
        if (noisyBits > 0) {
            encodeBits(noisyBits, value);
            value >>>= noisyBits;
        }
        long range = 0L;
        int nBits = 0;
        while (value > range) {
            encodeBit(false);
            value -= range + 1L;
            range = (range << 1) | 1L;
            nBits++;
        }
        encodeBit(true);
        encodeBits(nBits, value);
    }

    /**
     * Similar to {@link #encodeUnsignedVarBits(long,int)} but allowing for negative
     * numbers by prepending a sign bit.
     *
     * @param value     the number to encode
     * @param noisyBits the number of lower bits considered noisy
     *
     * @see BitInputStream#decodeSignedVarBits(int)
     */
    public final void encodeSignedVarBits(long value, int noisyBits) throws IOException {
        encodeBit(value < 0L);
        encodeUnsignedVarBits(value < 0L ? -value - 1L : value, noisyBits);
    }

    /**
     * Encode a long in the range 0..max (inclusive). For max = 2^n-1, this just
     * encodes n bits, but in general this is variable length encoding, with the
     * shorter codes for the central value range
     *
     * @param max   value is expected in the range [0..max]
     * @param value the value to encode
     *
     * @see BitInputStream#decodeBounded(long)
     */
    public final void encodeBounded(long max, long value) throws IOException {
        if (max < 0L || value < 0) {
            throw new IllegalArgumentException("encodeBounded expects positive values");
        }
        if (value > max) {
            throw new IllegalArgumentException("value out of range");
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
     * Encode a positive long-array making use of the fact that it is sorted and
     * unique. This is done, starting with the most significant bit, by recursively
     * encoding the number of values with the current bit being 0. This yields a
     * number of bits per value that only depends on the typical distance between
     * subsequent values and also benefits from clustering, because effectively a
     * local typical distance for the actual recursion level is used, not the global
     * one over the whole array.
     *
     * @param values the array to encode
     *
     * @see BitInputStream#decodeUniqueSortedArray()
     */
    public void encodeUniqueSortedArray(long[] values) throws IOException {
        int size = values.length;
        encodeUnsignedVarBits(size, 0);
        encodeUniqueSortedArray(values, 0, size, 0);
    }

    /**
     * Same as {@link #encodeUniqueSortedArray( long[] )}, but assuming that the
     * (sub-)size of the array is already known from context and does not need to be
     * encoded
     *
     * @param values        the array to encode
     * @param offset        position in this array where to start
     * @param size          number of values to encode
     * @param minLengthBits noisyBits used to encode bit-length of largest value
     */
    public void encodeUniqueSortedArray(long[] values, int offset, int size, int minLengthBits) throws IOException {
        if (size > 0) {
            long max = values[size - 1];
            int nBits = 0;
            while ((max >>>= 1) != 0L) {
                nBits++;
            }
            checkUniqueSortedArray(values, offset, size);
            encodeUnsignedVarBits(nBits, minLengthBits);
            encodeUniqueSortedArray(values, offset, size, nBits, 0L);
        }
    }

    private void checkUniqueSortedArray(long[] values, int offset, int size) {
        long lv = -1L;
        int end = offset + size;
        for (int i = offset; i < end; i++) {
            long v = values[i];
            if (lv >= v) {
                throw new IllegalArgumentException("checkUniqueSortedArray: not positive-sorted-unique at " + i);
            }
            lv = v;
        }
    }

    /**
     * Same as {@link #encodeUniqueSortedArray( long[], int, int, int )}, but
     * assuming that the most significant bit is known from context. This method
     * calls itself recursively down to subSize=1, where a fast shortcut kicks in to
     * encode the remaining bits of that remaining value-
     *
     * @param values     the array to encode
     * @param offset     position in this array where to start
     * @param subSize    number of values to encode
     * @param nextBitPos bit-position of the most significant bit
     * @param mask       should be 0 at recursion start
     */
    protected void encodeUniqueSortedArray(long[] values, int offset, int subSize, int nextBitPos, long mask)
            throws IOException {
        if (subSize == 1) // last-choice shortcut
        {
            // ugly here: inverse bit-order then without the last-choice shortcut,
            // but we do it that way for performance
            encodeBits(nextBitPos + 1, values[offset]);
            return;
        }
        if (nextBitPos < 0L) {
            return;
        }

        long nextBits = 1L << nextBitPos;
        long data = mask & values[offset];
        mask |= nextBits;

        // count 0-bit-fraction
        int i = offset;
        int end = offset + subSize;
        for (; i < end; i++) {
            if ((values[i] & mask) != data) {
                break;
            }
        }
        int size1 = i - offset;
        int size2 = subSize - size1;

        if (subSize > nextBits) {
            long min = subSize - nextBits;
            encodeBounded(nextBits - min, size1 - min);
        } else {
            encodeBounded(subSize, size1);
        }
        if (size1 > 0) {
            encodeUniqueSortedArray(values, offset, size1, nextBitPos - 1, mask);
        }
        if (size2 > 0) {
            encodeUniqueSortedArray(values, i, size2, nextBitPos - 1, mask);
        }
    }
}
