package btools.statcoding.huffman;

import java.io.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;

import junit.framework.TestCase;

public class RlH2CodingTest extends TestCase {

	private static long[] testLongs = new long[] { 0L, 0L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 3L, 17L };

	public void testRlH2Coding() throws IOException {

		// explicitly test also the "no symbol" case (nsymbols=0) and "only 1 Symbol"
		for (int nsymbols = 0; nsymbols <= testLongs.length; nsymbols++) {
			testRlH2Coding(nsymbols, 2);
			testRlH2Coding(nsymbols, 3);
			testRlH2Coding(nsymbols, 4);
		}
	}

	private void testRlH2Coding(int nsymbols, int minRunLength) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BitOutputStream bos = new BitOutputStream(baos)) {

			RlH2Encoder enc = new RlH2Encoder(17, minRunLength);

			for (int pass = 1; pass <= 2; pass++) { // 2-pass encoding!
				enc.init(bos);
				for (int i = 0; i < nsymbols; i++) {
					enc.encodeValue(testLongs[i]);
				}
				enc.finish();
			}
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		try (BitInputStream bis = new BitInputStream(bais)) {

			RlH2Decoder dec = new RlH2Decoder();
			dec.init(bis);

			for (int i = 0; i < nsymbols; i++) {
				assertEquals("at nsymbols=" + nsymbols + " i=" + i + " minRunLength=" + minRunLength, dec.decodeValue(),
						testLongs[i]);
			}
		}
	}
}
