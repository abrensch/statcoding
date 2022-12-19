package btools.statcoding;

import java.io.IOException;


public class RlH2Decoder {
		
	private long maxValue;
	private long minRunlength;
  private long lastValue;
  private long repCount;
  private HuffmanDecoder[] decoders;
  private Long rleEscape = Long.valueOf( -1L );
  private BitInputStream bis;

	public RlH2Decoder( long maxValue, long minRunlength ) {
		this.maxValue = maxValue;
		this.minRunlength = minRunlength;
  	decoders = new HuffmanDecoder[(int)(maxValue+1)];
  	int n = decoders.length;
  	for( int i=0; i < n; i++ ) {
		  decoders[i] = new HuffmanDecoder() {
        @Override
        protected Object decodeObjectFromStream() throws IOException {
          return Long.valueOf( bis.decodeBounded(maxValue+1) - 1L );
        }
      };
    }
  }	

  public void init( BitInputStream bis ) throws IOException {
  	this.bis = bis;
  	int n = decoders.length;
  	for( int i=0; i < n; i++ ) {
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
