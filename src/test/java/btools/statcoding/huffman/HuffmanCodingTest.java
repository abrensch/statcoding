package btools.statcoding.huffman;

import java.io.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;

import junit.framework.TestCase;

public class HuffmanCodingTest extends TestCase {

    private static long[] testLongs = new long[] { 0L, 0L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 3L };

    public void testHuffmanCoding() throws IOException {

        // explicitly test also the "no symbol" case (nsymbols=0) and "only 1 Symbol"
        for (int nsymbols = 0; nsymbols < testLongs.length; nsymbols++) {
            testHuffmanCoding(nsymbols);
        }
    }

    private void testHuffmanCoding(int nsymbols) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (BitOutputStream bos = new BitOutputStream(baos)) {

            HuffmanEncoder enc = new HuffmanEncoder() {
                @Override
                protected void encodeObjectToStream(Object obj) throws IOException {
                    bos.encodeUnsignedVarBits(((Long) obj).longValue(), 0);
                }
            };

            for (int pass = 1; pass <= 2; pass++) { // 2-pass encoding!
                enc.init(bos);
                for (int i = 0; i < nsymbols; i++) {
                    enc.encodeObject(Long.valueOf(testLongs[i]));
                }
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (BitInputStream bis = new BitInputStream(bais)) {

            HuffmanDecoder dec = new HuffmanDecoder() {
                @Override
                protected Object decodeObjectFromStream() throws IOException {
                    return Long.valueOf(bis.decodeUnsignedVarBits(0));
                }
            };
            dec.init(bis);

            for (int i = 0; i < nsymbols; i++) {
                assertEquals(((Long) dec.decodeObject()).longValue(), testLongs[i]);
            }
        }
    }
}
