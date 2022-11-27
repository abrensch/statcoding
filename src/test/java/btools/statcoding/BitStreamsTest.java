package btools.statcoding;

import java.io.*;

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
}
