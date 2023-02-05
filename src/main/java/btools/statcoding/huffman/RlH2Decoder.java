package btools.statcoding.huffman;

import java.io.IOException;

import btools.statcoding.BitInputStream;

public class RlH2Decoder {
		
  private long maxValue;
  private long minRunlength;
  private long lastValue;
  private long repCount;
  private HuffmanDecoder[] decoders;
  private Long rleEscape = Long.valueOf( -1L );
  private BitInputStream bis;

  public void init( BitInputStream bis ) throws IOException {
  	this.bis = bis;

    maxValue = bis.decodeVarBits();
    minRunlength = bis.decodeVarBits();
  	int n = (int)(maxValue)+1;
  	decoders = new HuffmanDecoder[n];
  	for( int i=0; i < n; i++ ) {
	  decoders[i] = new HuffmanDecoder() {
        @Override
        protected Object decodeObjectFromStream() throws IOException {
          return Long.valueOf( bis.decodeBounded(maxValue+1) - 1L );
        }
      };
  	  decoders[i].init( bis );
    }

    repCount = 0;
    lastValue = 0L;
  }  

  public long decodeValue() throws IOException {
  	if ( repCount > 0 ) {
  		repCount--;
  		return lastValue;
    }
    HuffmanDecoder decoder = decoders[(int)lastValue];
    Long v = (Long)decoder.decodeObject();
	    if ( v.equals( rleEscape ) ) {
    	repCount = bis.decodeVarBits() + minRunlength - 1;
    	v = (Long)decoder.decodeObject();
	    if ( v.equals( rleEscape ) ) {
	    	throw new RuntimeException( "unexpected rle!" );
    	}    	
    }
    lastValue = v.longValue();
    return lastValue;
  }
}
