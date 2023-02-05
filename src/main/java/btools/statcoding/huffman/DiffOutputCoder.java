package btools.statcoding.huffman;

import java.io.IOException;

import btools.statcoding.BitOutputStream;

public final class DiffOutputCoder {
  private long lastValue;
  private long lastLastValue;
  private long repCount;

  private LongEncoder diffEncoder = new SignedLongEncoder();
  private LongEncoder repEncoder= new LongEncoder();

  public void init( BitOutputStream bos ) throws IOException
  {
    diffEncoder.init( bos );
    repEncoder.init( bos );
    lastValue = lastLastValue = repCount = 0;
  }

  public void writeDiffed(long v) throws IOException {
    if (v != lastValue && repCount > 0) {
      long d = lastValue - lastLastValue;
      lastLastValue = lastValue;
      
      diffEncoder.encodeLong( d );
      repEncoder.encodeLong( repCount-1 );

      repCount = 0;
    }
    lastValue = v;
    repCount++;
  }

  public void finish() throws IOException {
  	writeDiffed( lastValue + 1L );
  }
  

  private static class LongEncoder extends HuffmanEncoder
  {
    public void encodeLong( long value ) throws IOException
    {
      encodeObject( Long.valueOf( value ) );
    }
		
    @Override
    protected void encodeObjectToStream(Object obj) throws IOException {
      long lv = ((Long)obj).longValue();
      bos.encodeVarBits( lv );
    }
  }

  private static class SignedLongEncoder extends LongEncoder
  {
    @Override
    protected void encodeObjectToStream(Object obj) throws IOException {
      long lv = ((Long)obj).longValue();
      bos.encodeSignedVarBits( lv );
    }
  }
}
