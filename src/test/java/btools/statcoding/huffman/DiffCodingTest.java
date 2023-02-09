package btools.statcoding.huffman;

import java.io.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;

import junit.framework.TestCase;

public class DiffCodingTest extends TestCase {

	private static long[] testLongs = new long[] { 0L, 1L, 63738377475675L, Long.MAX_VALUE };

	public void testDiffCoding() throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BitOutputStream bos = new BitOutputStream(baos)) {

			DiffOutputCoder doc = new DiffOutputCoder();
			for (int pass = 1; pass <= 2; pass++) {
				doc.init(bos);
				for (long l : testLongs) {
					doc.writeDiffed(l);
				}
				doc.finish();
			}
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		try (BitInputStream bis = new BitInputStream(bais)) {

			DiffInputCoder dic = new DiffInputCoder();
			dic.init(bis);

			for (long l : testLongs) {
				assertEquals(dic.readDiffed(), l);
			}
		}
	}
}
