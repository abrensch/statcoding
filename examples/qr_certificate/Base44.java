import java.io.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;

/**
 * Probably not "Base44" in any defined sense, but encodes about 5,5 bits per
 * character of the alphanumeric-character set of QR-Codes. This character
 * set includes 45 characters, but we exclude the space-character for practical reasons,
 * so 44 characters left for us to encode our data with.
 *
 * This is an "inverse use" of decodeBounded/encodeBounded, because "decodeBounded"
 * is used for encoding and vice versa.
 *
 * Please note that an encode-decode cycle most probably grows the message
 * by one 0-byte, because there's no dedicated end-of-message logic
 */
public class Base44 {

    public final static String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$%*+-/.:";

    public static void encode(StringBuilder sb, byte[] ab) throws IOException {
        try (BitInputStream bis = new BitInputStream(ab)) {
            while (bis.hasMoreRealBits()) {
                sb.append(chars.charAt((int) bis.decodeBounded(chars.length() - 1)));
            }
        }
    }

    public static byte[] decode(String text, int offset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BitOutputStream bos = new BitOutputStream(baos)) {
            for (int i = offset; i < text.length(); i++) {
                char c = text.charAt(i);
                int idx = chars.indexOf(c);
                if (idx < 0) {
                    throw new IllegalArgumentException("not a base44 char: " + c);
                }
                bos.encodeBounded(chars.length() - 1, idx);
            }
        }
        return baos.toByteArray();
    }
}
