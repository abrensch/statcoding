import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * Decode some compact encoded lidar-tile and create a hill-shaded image,
 * optionally downscaled.
 */
public class CreateLidarImage {

    int[] data;

    private void decodeTile(File fileIn, int offset, int rowSize, int downscale) throws Exception {

        // decode the tile
        long t0 = System.currentTimeMillis();
        DEM1LidarTile tile = new DEM1LidarTile();
        try (InputStream is = new BufferedInputStream(new FileInputStream(fileIn))) {
            tile.readCompact(is);
        }
        long t1 = System.currentTimeMillis();
        System.out.println("decoding " + fileIn + " took " + (t1 - t0) + " ms");

        // copy/scale the data into the image array
        int[] tileData = tile.getData();
        for (int y = 0; y < 1000; y++) {
            for (int x = 0; x < 1000; x++) {
                int value = tileData[x + y * 1000];
                data[offset + (x / downscale) - (y / downscale) * rowSize] = value;
            }
        }
    }

    private void createImage(String dataType, String imageName, int minXkm, int minYkm, int maxXkm, int maxYkm, int downscale) throws Exception {
        int tileSize = 1000 / downscale;
        int sizeX = (maxXkm - minXkm) * tileSize;
        int sizeY = (maxYkm - minYkm) * tileSize;

        // create an empty ARGB image and its data buffer
        BufferedImage argbImage = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_ARGB);
        data = ((DataBufferInt) argbImage.getRaster().getDataBuffer()).getData();

        // now look for lidar squares in that region
        File dataDir = new File("data");
        File[] files = dataDir.listFiles();
        if ( files == null ) {
            throw new IllegalArgumentException( "cannot read files in data-dir: " + dataDir );
        }
        for (File f : files) {
            DEM1LidarTile tile = new DEM1LidarTile();
            if (tile.parseMetadataFromFileName(f.getName()) && tile.getDataType().equals(dataType)) {
                int xKm = tile.getXBaseKm();
                int yKm = tile.getYBaseKm();
                if (xKm >= minXkm && xKm < maxXkm && yKm >= minYkm && yKm < maxYkm) {
                	  int offset = (xKm - minXkm) * tileSize + ((maxYkm - yKm) * tileSize - 1) * sizeX;
                    decodeTile(f, offset, sizeX, downscale);
                }
            }
        }

        // apply poor man's hill-shading and rgb-encoding
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                int v0 = data[sizeX * y + x];
                int v1 = x > 0 && y < sizeY - 1 ? data[sizeX * (y + 1) + (x - 1)] : v0;
                int diff = (2 * (v1 - v0)) / downscale;
                int c = Math.max(0, Math.min(255, 128 - diff));
                int rgb = 0xff000000 | c | (c << 8) | (c << 16);
                data[y * sizeX + x] = rgb;
            }
        }

        // write the image in PNG format
        ImageIO.write(argbImage, "png", new FileOutputStream(imageName));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 7) {
            System.out.println(
                    "usage: java CreateLidarImage <dataType> <imageFileName> <minXkm> <minYkm> <maxXkm> <maxYkm> <downscale>");
            System.out.println("\nwhere: dataType = [dgm1|dom1] downscale = [1|2|4|..]");
            return;
        }
        new CreateLidarImage().createImage(args[0], args[1], Integer.parseInt(args[2]),
                Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]) );
    }
}
