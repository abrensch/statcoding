import btools.statcoding.BitInputStream;
import btools.statcoding.arithmetic.RlA2Decoder;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;


public class DecodeImage {

  private void processImage( String fileIn, String fileOut ) throws Exception {

    try ( BitInputStream bis = new BitInputStream( new FileInputStream( fileIn ) ) ) {
      int w = (int)bis.decodeVarBits();
      int h = (int)bis.decodeVarBits();
      int n = w*h;
      long[] colorArray = bis.decodeSortedArray();
      BufferedImage argbImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
      int[] data = ((DataBufferInt) argbImage.getRaster().getDataBuffer()).getData();

      RlA2Decoder decoder = new RlA2Decoder();
      decoder.init( bis );

      for(int i=0; i<n; i++) {
        int colorIdx = (int) decoder.decodeValue();
        data[i] = (int) colorArray[colorIdx];
      }
      ImageIO.write(argbImage, "png" ,new FileOutputStream( fileOut ) );
    }
  }

  public static void main(String args[]) throws Exception {
    new DecodeImage().processImage( args[0], args[1] );
  }
}
