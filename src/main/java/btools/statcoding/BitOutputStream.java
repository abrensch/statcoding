package btools.statcoding;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * BitOutputStream is a replacement for java.io.DataOutputStream extending it by
 * bitwise operations suitable for statistical encoding.
 *
 * It automatically re-aligns to byte-alignment as soon as any of the methods of
 * OutputStream or DataOutput or its own method 'encodeVarBytes' is called.
 */
public final class BitOutputStream extends OutputStream implements DataOutput {

    private int bits; // bits left in buffer
    private long b; // buffer word
    private long bytesWritten;

    private OutputStream out;
    private DataOutputStream dos; // created lazily if needed

    /**
     * Construct a BitOutputStream for the underlying OutputStream.
     *
     * Please note that BitOutputStream needs exclusive access to the underlying
     * OutputStream.because it is buffering bits that could otherwise come out of
     * order.
     *
     * @param value the bit to encode
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
     * Get the number of bits written so far.
     *
     * This includes padding bits from re-alignment.
     *
     * @return the number of bits.
     */
    public long getBitPosition() {
        return bytesWritten * 8L + bits;
    }

    // *******************************************
    // **** METHODS of java.util.OutputStream ****
    // *******************************************

    @Override
    public void write(int b) throws IOException {
        flushBufferAndReAlign();
        writeInternal(b);
    }

    /**
     * Flushes the underlying output stream
     *
     * Please note that this does not trigger re-alignment, so if this
     * BitoutputStream is not currently byte-aligned, then the bit-buffer is not
     * flushed
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

    // *****************************************
    // **** METHODS of java.util.DataOutput ****
    // *****************************************

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
    private final long moveSignBit(long value) {
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
     * @param nbits the number of bit to encode
     * @param value the value from whom to encode the lower nbits bits
     */
    public final void encodeBits(int nbits, long value) throws IOException {
        if (nbits > 0 && bits + nbits <= 64) {
            flushBuffer();
            long mask = 0xffffffffffffffffL >>> (64 - nbits);
            b |= (value & mask) << bits;
            bits += nbits;
            return;
        }
        if (nbits < 0 || nbits > 64) {
            throw new IllegalArgumentException("encodeBits: nbits out of rangs (0..64): " + nbits);
        }
        for (int i = 0; i < nbits; i++) { // buffer too small, slow fallback
            encodeBit((value & (1L << i)) != 0L);
        }
    }

    // ******************************************
    // **** Bitwise Variable Length Encoding ****
    // ******************************************

    public final void encodeUnsignedVarBits(long value, int noisybits) throws IOException {
        if (value < 0) {
            throw new IllegalArgumentException("encodeUnsignedVarBits expects non-negative values: " + value);
        }
        if (noisybits > 0) {
            encodeBits(noisybits, value);
            value >>>= noisybits;
        }
        long range = 0L;
        int nbits = 0;
        while (value > range) {
            encodeBit(false);
            value -= range + 1L;
            range = (range << 1) | 1L;
            nbits++;
        }
        encodeBit(true);
        encodeBits(nbits, value);
    }

    public final void encodeSignedVarBits(long value, int noisybits) throws IOException {
        encodeBit(value < 0L);
        encodeUnsignedVarBits(value < 0L ? -value - 1L : value, noisybits);
    }

    /**
     * encode a long in the range 0..max (inclusive). For max = 2^n-1, this just
     * encodes n bits, but in general this is variable length encoding, with the
     * shorter codes for the central value range
     */
    public final void encodeBounded(long max, long value) throws IOException {
        if (max < 0L || value < 0) {
            throw new IllegalArgumentException("encodeBounded expects positive values");
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
     * encode a positive long-array making use of the fact that it is sorted and
     * unique. This is done, starting with the most significant bit, by recursively
     * encoding the number of values with the current bit being 0. This yields an
     * number of bits per value that only depends on the typical distance between
     * subsequent values and also benefits from clustering, because effectively a
     * local typical distance for the actual recursion level is used, not the global
     * one over the whole array.
     *
     * @param values the array to encode
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
     * @param minLengthBits noisyBits used to encode bitlength of largest value
     */
    public void encodeUniqueSortedArray(long[] values, int offset, int size, int minLengthBits) throws IOException {
        if (size > 0) {
            long max = values[size - 1];
            int nbits = 0;
            while ((max >>>= 1) != 0L) {
                nbits++;
            }
            encodeUnsignedVarBits(nbits, minLengthBits);
            encodeUniqueSortedArray(values, offset, size, 1L << nbits, 0L);
        }
    }

    /**
     * Same as {@link #encodeUniqueSortedArray( long[], int, int, int )}, but
     * assuming that the most significant bit is known from context. This method
     * calls itself recursively down to subsize=1, where a fast shortcut kicks in to
     * encode the remaining bits of that remaining value-
     *
     * @param values  the array to encode
     * @param offset  position in this array where to start
     * @param subsize number of values to encode
     * @param nextbit bitmask with the most significant bit set to 1
     * @param mask    should be 0 at recursion start
     */
    protected void encodeUniqueSortedArray(long[] values, int offset, int subsize, long nextbit, long mask)
            throws IOException {
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

        if (subsize > nextbit) {
            long max = nextbit;
            long min = subsize - nextbit;
            encodeBounded(max - min, size1 - min);
        } else {
            encodeBounded(subsize, size1);
        }
        if (size1 > 0) {
            encodeUniqueSortedArray(values, offset, size1, nextbit >>> 1, mask);
        }
        if (size2 > 0) {
            encodeUniqueSortedArray(values, i, size2, nextbit >>> 1, mask);
        }
    }
}
