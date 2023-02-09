package btools.statcoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
			writeByte((byte) (b & 0xffL));
			b >>>= 8;
			bits -= 8;
			bytesWritten++;
		}
	}

	@Override
	public void close() throws IOException {
		flushBuffer();
		if (bits > 0) {
			writeByte((byte) (b & 0xff));
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

	public final void encodeUnsignedVarBits(long value, int noisybits) throws IOException {
		if (value < 0) {
			throw new IllegalArgumentException("encodeUnsignedVarBits expects positive values: " + value);
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

	public final void encodeVarBytes(long value) throws IOException {
		long v = moveSignBit(value);
		for (;;) {
			long v7 = v & 0x7f;
			if ((v >>>= 7) == 0L) {
				writeByte((byte) (v7 & 0xffL));
				return;
			}
			writeByte((byte) ((v7 | 0x80L) & 0xffL));
		}
	}

	// re-arrange the bits of a signed long to make it better suited for var-length
	// coding
	private final long moveSignBit(long value) {
		return value < 0L ? 1L | ((-value - 1L) << 1) | 1 : value << 1;
	}

	public final void encodeBits(int nbits, long value) throws IOException {
		for (int i = 0; i < nbits; i++) {
			encodeBit((value & (1L << i)) != 0L);
		}
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

	public static void main(String[] args) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BitOutputStream bos = new BitOutputStream(baos)) {
			bos.encodeSignedVarBits(Long.MAX_VALUE, 0);
			bos.encodeSignedVarBits(Long.MIN_VALUE, 0);
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		try (BitInputStream bis = new BitInputStream(bais)) {

			assertEquals(bis.decodeSignedVarBits(0), Long.MAX_VALUE);
			assertEquals(bis.decodeSignedVarBits(0), Long.MIN_VALUE);
		}
	}

	private static void assertTrue(boolean b) {
		if (!b)
			throw new RuntimeException("not true!");
	}

	private static void assertEquals(long l1, long l2) {
		if (l1 != l2)
			throw new RuntimeException("found " + l1 + " but expected " + l2);
	}

}
