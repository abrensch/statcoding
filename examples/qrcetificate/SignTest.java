import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import javax.crypto.*;

public class SignTest
{
    public static void main( String[] args ) throws Exception
    {
    	  char[] storePwd = "geheim".toCharArray();
        KeyStore ks = KeyStore.getInstance("pkcs12");
        try ( InputStream is = new FileInputStream("testpair.p12") ) {
            ks.load(is, storePwd);
        }

        // find the private key of the signer
        Key k = ks.getKey( "testpair", storePwd );
        if ( ! (k instanceof PrivateKey) )
        {
          throw new IllegalArgumentException( "private key for testpair not found in keystore testpair.p12" );
        }
        PrivateKey privateKey = (PrivateKey)k;

        // Test-Message
        
        String text = "Hallo Du Depp";
        byte[] msg = text.getBytes( "UTF-8" );

        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(msg);
        byte[] signature = ecdsaSign.sign();

System.out.println( "signature size=" + signature.length );


        // find the public key of the recipient
        Certificate c = ks.getCertificate( "testpair" );
        if ( c == null )
        {
          throw new IllegalArgumentException( "certificate for testpair not found in keystore testpair.p12" );
        }
        PublicKey publicKey = c.getPublicKey();
        
        
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(msg);
        boolean result = ecdsaVerify.verify(signature);

System.out.println( "verification result=" + result );

    }

}
