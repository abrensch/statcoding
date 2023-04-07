import java.io.*;
import java.util.*;
import java.util.zip.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;
import btools.statcoding.PrefixedBitInputStream;
import btools.statcoding.PrefixedBitOutputStream;

/**
 * Encode the dummy certificate
 */
public class EncodeDummyCertificate {

    public static void main(String[] args) throws Exception {

        CovidCertificate c = DummyCertificateFactory.getInstance();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BitOutputStream bos = new BitOutputStream(baos)) {
            c.writeToStream(bos);
        }
        byte[] payloadData = baos.toByteArray();
        byte[] signatureData = new SignTool().createSignature(payloadData);

        baos = new ByteArrayOutputStream();
        try (BitOutputStream bos = new BitOutputStream(baos)) {
            bos.encodeSizedByteArray(payloadData);
            bos.encodeSizedByteArray(signatureData);
        }
        byte[] ab = baos.toByteArray();

        StringBuilder sb = new StringBuilder("010107"); // EU's DCC prefix is HC1: ...
        Base10.encode(sb, ab);
        System.out.println(sb.toString());
    }
}
