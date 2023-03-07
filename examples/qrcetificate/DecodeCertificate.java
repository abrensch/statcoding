import java.io.*;
import btools.statcoding.*;

/**
 * Decode a certificate and verify the digital signature
 */
public class DecodeCertificate {

    public static void main(String[] args) throws IOException {
    	
    	  if ( args.length != 1 || !args[0].startsWith( "HC7:" ) ) {
    	  	  System.out.println( "usage:\njava DecodeCertificate HC7:xxx..." );
    	  	  return;
    	  }
        byte[] ab = Base44.decode( args[0], 4 );
        CovidCertificate c = null;
    	
        try ( BitInputStream bis = new BitInputStream( new ByteArrayInputStream( ab ) ) ) {

        	  int payloadSize = bis.decodeVarBytes();
        	  byte[] payloadData = new byte[payloadSize];
        	  bis.readFully( payloadData );
        	  
            try ( BitInputStream isPayload = new BitInputStream( new ByteArrayInputStream( payloadData ) ) ) {
        	      c = new CovidCertificate( isPayload );
        	  }

        	  int signatureSize = bis.decodeVarBytes();
        	  byte[] signatureData = new byte[signatureSize];
        	  bis.readFully( payload );

            boolean signatureValid = SignTool.verifySignature( payloadData, signatureData );            
        	  System.out.println( "Signature-Check: " + ( signatureValid ? "valid" : "********** INVALID !! ***********" ) );
        	  System.out.println( new CovidCertificate( bis ) );
        }
    }
}
