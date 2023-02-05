package btools.statcoding;

import java.io.*;
import java.util.*;

import junit.framework.TestCase;

public class BitStreamsTest extends TestCase {

  private static long[] testLongs = new long[] { 0L, 1L, 63738377475675L, Long.MAX_VALUE };

  public void testBitStreams() throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try( BitOutputStream bos = new BitOutputStream( baos ) ) {
      bos.encodeBit( true );
      bos.encodeBit( false );
      for( long l : testLongs ) {
        bos.encodeVarBits( l );
        bos.encodeSignedVarBits( l );
        bos.encodeSignedVarBits( -l );
      }
      bos.encodeSignedVarBits( Long.MIN_VALUE );
    }
    ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
    try( BitInputStream bis = new BitInputStream( bais ) ) {

      assertTrue ( bis.decodeBit() );
      assertTrue ( !bis.decodeBit() );
      for( long l : testLongs ) {
        assertEquals ( bis.decodeVarBits(), l );
        assertEquals ( bis.decodeSignedVarBits(), l );
        assertEquals ( bis.decodeSignedVarBits(), -l );
      }
      assertEquals( bis.decodeSignedVarBits(), Long.MIN_VALUE );
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

    for( int  i=0; i< size; i++ ) {
      values[i] += i; // force uniqueness
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try( BitOutputStream bos = new BitOutputStream( baos ) ) {
      bos.encodeSortedArray( values );
    }
    ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
    try( BitInputStream bis = new BitInputStream( bais ) ) {
      long[] decodedValues = bis.decodeSortedArray();
      assertTrue ( Arrays.equals( values, decodedValues ) );
    }
  }

  public void testArrayEncodeDecode() throws IOException {
    Random rand = new Random();
    int size = 62;
    long[] values = new long[size];
    long mask = 1L;
    for (int i = 0; i < size; i++) {
      values[i] = ( rand.nextLong() & mask ) + 1L;
      mask = 1L | (mask <<= 1);
    }
    long[] v2 = new long[size];
    long sum = 0L;
    for (int i = 0; i < size; i++) {
      sum += values[i];
      v2[i] = sum;
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try( BitOutputStream bos = new BitOutputStream( baos ) ) {
      bos.encodeSortedArray( v2 );
    }
    ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
    try( BitInputStream bis = new BitInputStream( bais ) ) {
      long[] decodedValues = bis.decodeSortedArray();
      long lastValue = 0L;
      for( int i=0; i< decodedValues.length; i++ ) {
        long diff = decodedValues[i] - lastValue;
        lastValue = decodedValues[i];
        decodedValues[i] = diff;
      }
      assertTrue ( Arrays.equals( values, decodedValues ) );
    }
  }
}
