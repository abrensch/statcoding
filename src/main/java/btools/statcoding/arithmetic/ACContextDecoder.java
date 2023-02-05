package btools.statcoding.arithmetic;

import java.io.IOException;
import java.util.*;

import btools.statcoding.BitInputStream;

/**
 * Decodes stats and symbols from arithmentic decoder
 */
public final class ACContextDecoder {

  // The underlying decoder
  private ArithmeticDecoder decoder;

  private long[] stats;
  private long[] idx2symbol;

  public void init( ArithmeticDecoder decoder ) throws IOException {

    this.decoder = decoder;

    BitInputStream bis = decoder.getInputStream();

    // decode statistics
    int size = (int)bis.decodeVarBits();
    if ( size > 1 ) { // need no stats for size = 1
    	stats = new long[size];      	
      bis.decodeSortedArray(stats, size, 0 );
    }
    if ( size > 0 ) {
    	idx2symbol = new long[size]; 
      bis.decodeSortedArray(idx2symbol, size, 3 );
    }
  }

  public long read() throws IOException {
 
    if ( idx2symbol == null ) {
    	throw new IllegalArgumentException( "cannot read (no symbols)" );
    }
    if ( stats == null ) {
    	return idx2symbol[0];
    }

	  int idx = decoder.read(stats);
	  return idx2symbol[idx];
	}
}
