package btools.statcoding;

import java.io.*;
import java.util.*;

import junit.framework.TestCase;

public class BitStreamsTest extends TestCase {

    private static long[] testLongs = new long[] { 0L, 1L, 63738377475675L, Long.MAX_VALUE };

    public void testBitStreams() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (BitOutputStream bos = new BitOutputStream(baos)) {
            bos.encodeBit(true);
            bos.encodeBit(false);
            for (long l : testLongs) {
                bos.encodeUnsignedVarBits(l, 0);
                bos.encodeSignedVarBits(l, 0);
                bos.encodeSignedVarBits(-l, 0);
            }
            bos.encodeSignedVarBits(Long.MIN_VALUE, 0);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (BitInputStream bis = new BitInputStream(bais)) {

            assertTrue(bis.decodeBit());
            assertTrue(!bis.decodeBit());
            for (long l : testLongs) {
                assertEquals(bis.decodeUnsignedVarBits(0), l);
                assertEquals(bis.decodeSignedVarBits(0), l);
                assertEquals(bis.decodeSignedVarBits(0), -l);
            }
            assertEquals(bis.decodeSignedVarBits(0), Long.MIN_VALUE);
        }
    }

    public void testVarBytes() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (BitOutputStream bos = new BitOutputStream(baos)) {
            for (long l : testLongs) {
                bos.encodeVarBytes(l);
            }
            bos.encodeVarBytes(Long.MIN_VALUE);
            // test re-alignment
            bos.encodeSignedVarBits(1523L, 3);
            bos.encodeVarBytes(4711L);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (BitInputStream bis = new BitInputStream(bais)) {

            for (long l : testLongs) {
                assertEquals(bis.decodeVarBytes(), l);
            }
            assertEquals(bis.decodeVarBytes(), Long.MIN_VALUE);
            assertEquals(bis.decodeSignedVarBits(3), 1523L);
            assertEquals(bis.decodeVarBytes(), 4711L);
        }
    }

    public void testReAlignment() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (BitOutputStream bos = new BitOutputStream(baos)) {
            bos.encodeBits(3, 6L);
            bos.writeUTF("hallo");
            bos.encodeBits(5, 7L);
            bos.flush();
            bos.writeUTF("duda");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (BitInputStream bis = new BitInputStream(bais)) {

            assertEquals(bis.decodeBits(3), 6L);
            assertEquals(bis.readUTF(), "hallo");
            assertEquals(bis.decodeBits(5), 7L);
            assertEquals(bis.readUTF(), "duda");
        }
    }

    public void testSortedArrayEncodeDecode() throws IOException {
        Random rand = new Random();
        int size = 1000000;
        long[] values = new long[size];
        for (int i = 0; i < size; i++) {
            values[i] = rand.nextInt() & 0x0fffffffL;
        }

        // force nearby values
        values[5] = 175384L;
        values[8] = 175384L;
        values[15] = 275384L;
        values[18] = 275385L;

        Arrays.sort(values);

        for (int i = 0; i < size; i++) {
            values[i] += i; // force uniqueness
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BitOutputStream bos = new BitOutputStream(baos)) {
            bos.encodeUniqueSortedArray(values);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (BitInputStream bis = new BitInputStream(bais)) {
            long[] decodedValues = bis.decodeUniqueSortedArray();
            assertTrue(Arrays.equals(values, decodedValues));
        }
    }

    public void testArrayEncodeDecode() throws IOException {
        Random rand = new Random();
        int size = 62;
        long[] values = new long[size];
        long mask = 1L;
        for (int i = 0; i < size; i++) {
            values[i] = (rand.nextLong() & mask) + 1L;
            mask = 1L | (mask <<= 1);
        }
        long[] v2 = new long[size];
        long sum = 0L;
        for (int i = 0; i < size; i++) {
            sum += values[i];
            v2[i] = sum;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BitOutputStream bos = new BitOutputStream(baos)) {
            bos.encodeUniqueSortedArray(v2);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (BitInputStream bis = new BitInputStream(bais)) {
            long[] decodedValues = bis.decodeUniqueSortedArray();
            long lastValue = 0L;
            for (int i = 0; i < decodedValues.length; i++) {
                long diff = decodedValues[i] - lastValue;
                lastValue = decodedValues[i];
                decodedValues[i] = diff;
            }
            assertTrue(Arrays.equals(values, decodedValues));
        }
    }
}
