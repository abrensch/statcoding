package btools.statcoding.huffman;

import java.io.IOException;

import btools.statcoding.BitOutputStream;

public class RlH2Encoder {
		
  private long maxValue;
  private long minRunlength;
  private long lastValue;
  private long contextValue;
  private long repCount;
  private HuffmanEncoder[] encoders;
  private Long rleEscape = Long.valueOf( -1L );
  private int pass;
  private BitOutputStream bos;

  private long rlebits;
  private long rlecount;

  public RlH2Encoder( long maxValue, long minRunlength ) {
    this.maxValue = maxValue;
    this.minRunlength = minRunlength;
    encoders = new HuffmanEncoder[(int)(maxValue+1)];
  	int n = encoders.length;
  	for( int i=0; i < n; i++ ) {
      encoders[i] = new HuffmanEncoder() {
        @Override
        protected void encodeObjectToStream(Object obj) throws IOException {
          bos.encodeBounded( maxValue+1, ((Long)obj).longValue() + 1L );
        }
      };
    }
  }


  public void init( BitOutputStream bos ) throws IOException {
  	if ( ++pass == 2 ) {
      bos.encodeVarBits(maxValue);
      bos.encodeVarBits(minRunlength);
  	}
  	this.bos = bos;
  	int n = encoders.length;
  	for( int i=0; i < n; i++ ) {
  	  encoders[i].init( bos );
  	}
    repCount = 0;
    lastValue = 0L;
    contextValue = 0L;
  }
  

  public void encodeValue( long value ) throws IOException {
  
    if ( value < 0L || value > maxValue ) {
      throw new IllegalArgumentException( "invalid value: " + value + " (maxValue=" + maxValue + ")" );
    }  
    if (value != lastValue ) {
      if ( repCount >= minRunlength ) {
        encoders[(int)contextValue].encodeObject( rleEscape ); // prefix runlength escape
        if ( pass == 2 ) {
          long rlestart = bos.getBitPosition();
          bos.encodeVarBits( repCount-minRunlength );
          rlebits += bos.getBitPosition() - rlestart;
          rlecount++;
        }
        repCount = 1;
      }
      while( repCount > 0 ) {
        encoders[(int)contextValue].encodeObject( Long.valueOf( lastValue ) );
        contextValue = lastValue;
        repCount--;
      }
    }
    lastValue = value;
    repCount++;
  }


  public void finish() throws IOException {
    encodeValue( (lastValue+1) % (maxValue+1) );
  }
  
  public String getStats() {
  	StringBuilder sb = new StringBuilder();
  	int n = encoders.length;
  	double totalEntropy = 0.;
  	for( int i=0; i < n; i++ ) {
      String stats = encoders[i].getStats();
      sb.append( stats ).append( "\n" );
      double entropy = Double.parseDouble( stats.substring( stats.lastIndexOf( "=" ) + 1 ) );
      totalEntropy += entropy;
    }
    return sb.toString() + "totalEntropy=" + totalEntropy + " rlebits=" + rlebits + " rlecount=" + rlecount;
  }
}
