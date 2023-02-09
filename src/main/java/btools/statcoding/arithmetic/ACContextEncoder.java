package btools.statcoding.arithmetic;

import java.io.IOException;
import java.util.*;

import btools.statcoding.BitOutputStream;

/**
 * Encodes symbols and writes to an arithmetic-coded bit stream. Not
 * thread-safe.
 * 
 * @see ArithmeticDecoder
 */
public final class ACContextEncoder {

	// The underlying encoder
	private ArithmeticEncoder encoder;

	private TreeMap<Integer, long[]> freqs = new TreeMap<>();

	private long[] stats;
	private int pass;

	public void init(ArithmeticEncoder encoder) throws IOException {

		this.encoder = encoder;

		if (++pass == 2) {
			// prepare frequency table
			int size = freqs.size();
			stats = new long[size];
			long[] idx2symbol = new long[size];
			int idx = 0;
			for (Integer iSymbol : freqs.keySet()) {
				long[] freq = freqs.get(iSymbol);
				stats[idx] = freq[0];
				freq[1] = idx;
				idx2symbol[idx] = iSymbol.intValue();
				idx++;
			}
			ArithmeticCoderBase.createStatsFromFrequencies(stats);
			BitOutputStream bos = encoder.getOutputStream();

			// encode statistics
			bos.encodeUnsignedVarBits(size, 0);
			if (size > 1) { // need no stats for size = 1
				bos.encodeUniqueSortedArray(stats, 0, size, 0);
			}
			bos.encodeUniqueSortedArray(idx2symbol, 0, size, 3);
		}
	}

	public void write(int symbol) throws IOException {
		Integer iSymbol = Integer.valueOf(symbol);
		if (pass < 2) {
			long[] current = freqs.get(iSymbol);
			if (current == null) {
				current = new long[2]; // [frequency, index]
				freqs.put(iSymbol, current);
			}
			current[0]++;
		} else {
			long[] current = freqs.get(iSymbol);
			if (current == null) {
				throw new IllegalArgumentException("symbol " + symbol + " is unkown from pass1");
			}
			encoder.write(stats, (int) current[1]);
		}
	}
}
