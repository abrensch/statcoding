import btools.statcoding.BitOutputStream;
import btools.statcoding.arithmetic.RlA2Encoder;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Simple example for image compression.
 * <br><br>
 * It first extracts the palette, indexing all distinct ARGB values, and then
 * encodes the series of these index values using a standard model ( 2nd order
 * arithmetic encoding with fixed statistics + run length escape )
 * <br><br>
 * It works o.k. over the whole range of images types (graphics, map-tiles,
 * screenshots, photos) and mostly beats PNG in compression ratio.
 * <br><br>
 * However, for a large number of distinct colors it's not that good in terms of
 * performance and memory footprint.
 * <br><br>
 * So this is NOT a proposal for a new image format (there are plenty already)
 * but just a demo on how the stat-coding library could work also for other kinds
 * of data.
 */
public class EncodeImage {

    private void processImage(File fileIn, File fileOut) throws Exception {

        // read image and convert to a standard ARGB representation
        BufferedImage inputImage = ImageIO.read(fileIn);
        BufferedImage argbImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = argbImage.createGraphics();
        g.drawImage(inputImage, 0, 0, null);
        g.dispose();
        int w = argbImage.getWidth();
        int h = argbImage.getHeight();
        int n = w * h;
        int[] data = ((DataBufferInt) argbImage.getRaster().getDataBuffer()).getData();

        // extract the color palette and sort by ARGB value
        SortedSet<Long> colorSet = new TreeSet<>();
        for (int i = 0; i < n; i++) {
            colorSet.add(data[i] & 0xffffffffL);
        }
        SortedMap<Long, Long> colorMap = new TreeMap<>();
        long[] colorArray = new long[colorSet.size()];
        for (Long col : colorSet) {
            int idx = colorMap.size();
            colorArray[idx] = col;
            colorMap.put(col, (long)idx);
        }

        try (BitOutputStream bos = new BitOutputStream(new BufferedOutputStream( new FileOutputStream(fileOut)))) {

            // encode the image dimensions
            bos.encodeUnsignedVarBits(w, 9);
            bos.encodeUnsignedVarBits(h, 9);

            // encode the color palette
            bos.encodeUniqueSortedArray(colorArray);

            // encode the series of color index values using RlA2Encoder
            // (=2nd order arithmetic encoding + run-length-escape)
            // using 2-pass encoding (pass1: collect stats, pass2: encode)
            RlA2Encoder encoder = new RlA2Encoder(colorArray.length - 1, 8);
            for (int pass = 1; pass <= 2; pass++) {
                encoder.init(bos);
                for (int i = 0; i < n; i++) {
                    long col = data[i] & 0xffffffffL;
                    encoder.encodeValue(colorMap.get(col));
                }
                encoder.finish();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new EncodeImage().processImage(new File(args[0]), new File(args[1]));
    }
}
