import java.io.*;
import btools.statcoding.*;

/**
 * Decode a certificate
 */
public class DecodeCertificate {

    public static void main(String[] args) throws IOException {
    	
    	  if ( args.length != 1 || !args[0].startsWith( "HC7:" ) ) {
    	  	  System.out.println( "usage:\njava DecodeCertificate HC7:xxx..." );
    	  	  return;
    	  }
        byte[] ab = Base44.decode( args[0], 4 );
    	
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ( BitInputStream bis = new BitInputStream( new ByteArrayInputStream( ab ) ) ) {
        	  System.out.println( new CovidCertificate( bis ) );
        }
    }
}
