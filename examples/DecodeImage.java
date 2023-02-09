import btools.statcoding.BitInputStream;
import btools.statcoding.arithmetic.RlA2Decoder;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Decoding twin to EncodeImage
 * 
 * @see EncodeImage
 */
public class DecodeImage {

	private void processImage(String fileIn, String fileOut) throws Exception {

		try (BitInputStream bis = new BitInputStream(new FileInputStream(fileIn))) {

			// decode the image dimensions
			int w = (int) bis.decodeUnsignedVarBits(9);
			int h = (int) bis.decodeUnsignedVarBits(9);
			int n = w * h;

			// decode the color palette
			long[] colorArray = bis.decodeUniqueSortedArray();

			// create an empty ARGB image
			BufferedImage argbImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			int[] data = ((DataBufferInt) argbImage.getRaster().getDataBuffer()).getData();

			// decode the color index values and fill the image
			RlA2Decoder decoder = new RlA2Decoder();
			decoder.init(bis);
			for (int i = 0; i < n; i++) {
				int colorIdx = (int) decoder.decodeValue();
				data[i] = (int) colorArray[colorIdx];
			}

			// re-write the image in PNG format
			ImageIO.write(argbImage, "png", new FileOutputStream(fileOut));
		}
	}

	public static void main(String args[]) throws Exception {
		new DecodeImage().processImage(args[0], args[1]);
	}
}
