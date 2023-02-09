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
	private int eofBits; // dummy bits read after eof
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
	public final long decodeUnsignedVarBits(int noisybits) throws IOException {
		long noisyValue = decodeBits(noisybits);
		long range = decodeRange();
		return noisyValue | ((range + decodeBounded(range)) << noisybits);
	}

	private long decodeRange() throws IOException {
		long range = 0L;
		while (!decodeBit()) {
			if (range == -1L) {
				throw new RuntimeException("range overflow");
			}
			range = (range << 1) | 1L;
		}
		return range;
	}

	public final long decodeSignedVarBits(int noisybits) throws IOException {
		boolean isNegative = decodeBit();
		long lv = decodeUnsignedVarBits(noisybits);
		return isNegative ? -lv - 1L : lv;
	}

	private final long restoreSignBit(long value) {
		return (value & 1L) == 0L ? value >>> 1 : -(value >>> 1) - 1L;
	}

	public final long decodeBits(int count) throws IOException {
		if (count == 0) {
			return 0L;
		}
		fillBuffer();
		long mask = 0xffffffffffffffffL >>> (64 - count);
		long value = b & mask;
		b >>>= count;
		bits -= count;
		return value;
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
	 * (except that current bit is provided by postion, not bitmask) <br>
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
			while (nextbitpos >= 0) {
				if (decodeBit()) {
					value |= 1L << nextbitpos;
				}
				nextbitpos--;
			}
			values[offset] = value;
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
			decodeUniqueSortedArray(values, offset + size1, size2, nextbitpos - 1, value | (1L << nextbitpos));
		}
	}

	private void fillBuffer() throws IOException {
		while (bits < 56) {
			int nextByte = read();

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
}
