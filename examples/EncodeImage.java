import btools.statcoding.*;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;

public class EncodeImage {

  private void processImage( String fileIn, String fileOut ) throws Exception {

    File file = new File( fileIn );
    BufferedImage image = ImageIO.read(file);
    ColorModel model = image.getColorModel();
        
    int w = image.getWidth();
    int h = image.getHeight();

    WritableRaster raster = image.getRaster();

    BitOutputStream bos = new BitOutputStream( new FileOutputStream( fileOut ) ); 
        
    for( int colShift = 0; colShift < 32; colShift += 8 ) {
      DiffOutputCoder diffCoder = new DiffOutputCoder();
      for( int pass=1; pass <=2; pass++ ) {
        diffCoder.init( bos );
        for(int i=0; i<w; i++) {
          for(int j=0; j<h; j++)  {
                
            Object data = raster.getDataElements(i, j, null);
            int argb = model.getRGB(data);
                
            int v = (argb >> colShift) & 0xff;
                
            diffCoder.writeDiffed(v );
          }
        }
        diffCoder.finish();
      }
    }
    bos.close();
  }

  public static void main(String args[]) throws Exception {
    new EncodeImage().processImage( args[0], args[1] );
  }
} 