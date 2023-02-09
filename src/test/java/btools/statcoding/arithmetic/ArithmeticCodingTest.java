package btools.statcoding.arithmetic;

import java.io.*;
import java.util.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;

import junit.framework.TestCase;

public class ArithmeticCodingTest extends TestCase {

	public void testArithmeticCoding() throws IOException {
		testArithmeticCoding(256, 100);
	}

	private void testArithmeticCoding(int symbolRange, int nsymbols) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		long[] freqs = new long[symbolRange];
		Random rnd = new Random(123); // fixed seed
		for (int i = 0; i < nsymbols; i++) {
			int nextSymbol = rnd.nextInt(symbolRange);
			freqs[nextSymbol]++;
		}

		ArithmeticCoderBase.createStatsFromFrequencies(freqs);

		try (BitOutputStream bos = new BitOutputStream(baos)) {
			ArithmeticEncoder enc = new ArithmeticEncoder(bos);
			rnd = new Random(123); // fixed seed
			for (int i = 0; i < nsymbols; i++) {
				int nextSymbol = rnd.nextInt(symbolRange);
				enc.write(freqs, nextSymbol);
			}
			enc.finish();
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		try (BitInputStream bis = new BitInputStream(bais)) {

			ArithmeticDecoder dec = new ArithmeticDecoder(bis);
			rnd = new Random(123); // fixed seed

			for (int i = 0; i < nsymbols; i++) {
				int expectedSymbol = rnd.nextInt(symbolRange);
				int decodedSymbol = dec.read(freqs);
				assertEquals(expectedSymbol, decodedSymbol);
			}
		}
	}
}
