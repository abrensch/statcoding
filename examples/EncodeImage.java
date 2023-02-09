import btools.statcoding.BitOutputStream;
import btools.statcoding.arithmetic.RlA2Encoder;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

public class EncodeImage {

	private void processImage(String fileIn, String fileOut) throws Exception {

		File file = new File(fileIn);
		BufferedImage inputImage = ImageIO.read(file);
		BufferedImage argbImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = argbImage.createGraphics();
		g.drawImage(inputImage, 0, 0, null);
		g.dispose();

		int w = argbImage.getWidth();
		int h = argbImage.getHeight();
		int n = w * h;

		int[] data = ((DataBufferInt) argbImage.getRaster().getDataBuffer()).getData();

		SortedSet<Long> colorSet = new TreeSet<>();
		for (int i = 0; i < n; i++) {
			Long col = Long.valueOf(data[i] & 0xffffffffL);
			colorSet.add(col);
		}
		SortedMap<Long, Long> colorMap = new TreeMap<>();
		long[] colorArray = new long[colorSet.size()];
		for (Long col : colorSet) {
			int idx = colorMap.size();
			colorArray[idx] = col.longValue();
			colorMap.put(col, Long.valueOf(idx));
		}
		colorSet = null;

		try (BitOutputStream bos = new BitOutputStream(new FileOutputStream(fileOut))) {
			bos.encodeUnsignedVarBits(w, 9);
			bos.encodeUnsignedVarBits(h, 9);
			bos.encodeUniqueSortedArray(colorArray);

			RlA2Encoder encoder = new RlA2Encoder(colorArray.length - 1, 8);

			for (int pass = 1; pass <= 2; pass++) {
				encoder.init(bos);
				for (int i = 0; i < n; i++) {
					Long col = Long.valueOf(data[i] & 0xffffffffL);
					encoder.encodeValue(colorMap.get(col));
				}
				encoder.finish();
			}
		}
	}

	public static void main(String args[]) throws Exception {
		new EncodeImage().processImage(args[0], args[1]);
	}
}
