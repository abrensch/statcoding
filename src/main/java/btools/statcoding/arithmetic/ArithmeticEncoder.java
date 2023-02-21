package btools.statcoding.arithmetic;

import java.io.IOException;

import btools.statcoding.BitOutputStream;

/**
 * Encodes symbols and writes to an arithmetic-coded bit stream.
 * <br><br>
 * This code is mostly taken from:
 * https://github.com/nayuki/Reference-arithmetic-coding
 * <br><br>
 * Please note that this needs exclusive access to the underlying BitOutputStream
 * after the first symbol is encoded. Underlying BitOutputStream should be
 * closed when the arithmetic stream is finished, cause re-aligning the
 * bitstream is not tested.
 *
 * @see ArithmeticDecoder
 */
public final class ArithmeticEncoder extends ArithmeticCoderBase {

    // The underlying bit output stream (not null).
    private final BitOutputStream output;

    // Number of saved underflow bits.
    private long numUnderflow;

    /**
     * Constructs an arithmetic encoder based on the specified bit stream.
     * 
     * @param out     the bit output stream to write to
     */
    public ArithmeticEncoder(BitOutputStream out) {
        output = out;
    }

    public BitOutputStream getOutputStream() {
        return output;
    }

    /**
     * Encodes the specified symbol based on the specified frequency table. Also
     * updates this arithmetic coder's state and may write out some bits.
     * 
     * @param stats  the (integrated) frequency table to use
     * @param symbol the symbol to encode
     * @throws IllegalArgumentException if the symbol has zero frequency or the
     *                                  frequency table's total is too large
     * @throws IOException              if an I/O exception occurred
     */
    public void write(long[] stats, int symbol) throws IOException {
        update(stats, symbol);
    }

    /**
     * Terminates the arithmetic coding by flushing any buffered bits, so that the
     * output can be decoded properly. It is important that this method must be
     * called at the end of each encoding process.
     * <p>
     * Note that this method merely writes data to the underlying output stream but
     * does not close it.
     * </p>
     * 
     * @throws IOException if an I/O exception occurred
     */
    public void finish() throws IOException {
        output.encodeBit(true);
    }

    protected void shift() throws IOException {
        int bit = (int) (low >>> (numStateBits - 1));
        output.encodeBit(bit != 0);

        // Write out the saved underflow bits
        for (; numUnderflow > 0; numUnderflow--)
            output.encodeBit(bit == 0);
    }

    protected void underflow() {
        numUnderflow++;
    }

}
