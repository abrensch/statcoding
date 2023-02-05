package btools.statcoding.arithmetic;

import java.io.IOException;
import java.util.Objects;

import btools.statcoding.BitOutputStream;

/**
 * Encodes symbols and writes to an arithmetic-coded bit stream. Not thread-safe.
 * @see ArithmeticDecoder
 */
public final class ArithmeticEncoder extends ArithmeticCoderBase {
	
	// The underlying bit output stream (not null).
	private BitOutputStream output;
	
	// Number of saved underflow bits. This value can grow without bound,
	// so a truly correct implementation would use a BigInteger.
	private int numUnderflow;
	
	
	/**
	 * Constructs an arithmetic coding encoder based on the specified bit output stream.
	 * @param numBits the number of bits for the arithmetic coding range
	 * @param out the bit output stream to write to
	 * @throws NullPointerException if the output stream is {@code null}
	 * @throws IllegalArgumentException if stateSize is outside the range [1, 62]
	 */
	public ArithmeticEncoder(BitOutputStream out) {
		output = out;
	}
	
	
	public BitOutputStream getOutputStream() {
	  return output;
	}
	
	/**
	 * Encodes the specified symbol based on the specified frequency table.
	 * Also updates this arithmetic coder's state and may write out some bits.
	 * @param stats the frequency table to use
	 * @param symbol the symbol to encode
	 * @throws IllegalArgumentException if the symbol has zero frequency
	 * or the frequency table's total is too large
	 * @throws IOException if an I/O exception occurred
	 */
	public void write(long[] stats, int symbol) throws IOException {
		update(stats, symbol);
	}
	
	
	/**
	 * Terminates the arithmetic coding by flushing any buffered bits, so that the output can be decoded properly.
	 * It is important that this method must be called at the end of the each encoding process.
	 * <p>Note that this method merely writes data to the underlying output stream but does not close it.</p>
	 * @throws IOException if an I/O exception occurred
	 */
	public void finish() throws IOException {
		output.encodeBit(true);
	}
	
	
	protected void shift() throws IOException {
		int bit = (int)(low >>> (numStateBits - 1));
		output.encodeBit(bit != 0);
		
		// Write out the saved underflow bits
		for (; numUnderflow > 0; numUnderflow--)
			output.encodeBit(bit == 0);
	}
	
	
	protected void underflow() {
		if (numUnderflow == Integer.MAX_VALUE)
			throw new ArithmeticException("Maximum underflow reached");
		numUnderflow++;
	}
	
}
