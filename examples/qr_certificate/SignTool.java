import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import javax.crypto.*;

public class SignTool {
    private KeyStore ks;
    private static String storeAlias = "testpair";
    private static char[] storePwd = "geheim".toCharArray();

    public SignTool() throws Exception {
        ks = KeyStore.getInstance("pkcs12");
        try (InputStream is = new FileInputStream("testpair.p12")) {
            ks.load(is, storePwd);
        }
    }

    public boolean verifySignature(byte[] payloadData, byte[] signatureData) throws Exception {
        // find the public key of the recipient
        Certificate c = ks.getCertificate(storeAlias);
        if (c == null) {
            throw new IllegalArgumentException("certificate for alias " + storeAlias + " not found");
        }
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
        ecdsaVerify.initVerify(c.getPublicKey());
        ecdsaVerify.update(payloadData);
        return ecdsaVerify.verify(signatureData);
    }

    public byte[] createSignature(byte[] payloadData) throws Exception {
        // find the private key of the signer
        Key k = ks.getKey(storeAlias, storePwd);
        if (!(k instanceof PrivateKey)) {
            throw new IllegalArgumentException("private key for alias " + storeAlias + " not found");
        }
        PrivateKey privateKey = (PrivateKey) k;

        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(payloadData);
        return ecdsaSign.sign();
    }
}
