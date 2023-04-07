import java.io.*;
import btools.statcoding.*;

/**
 * Decode a certificate and verify the digital signature
 */
public class DecodeCertificate {

    public static void main(String[] args) throws Exception {

        if (args.length != 1 || !args[0].startsWith("010107")) {
            System.out.println("usage:\njava DecodeCertificate 010107...");
            return;
        }
        byte[] ab = Base10.decode(args[0], 6);
        CovidCertificate c = null;

        try (BitInputStream bis = new BitInputStream(ab)) {

            byte[] payloadData = bis.decodeSizedByteArray();
            try (BitInputStream isPayload = new BitInputStream(payloadData)) {
                c = new CovidCertificate(isPayload);
            }

            byte[] signatureData = bis.decodeSizedByteArray();
            boolean signatureValid = new SignTool().verifySignature(payloadData, signatureData);
            System.out.println("Signature-Check: " + (signatureValid ? "valid" : "********** INVALID !! ***********"));
            System.out.println(c);
        }
    }
}
