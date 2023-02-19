import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import btools.statcoding.huffman.HuffmanDecoder;
import btools.statcoding.huffman.HuffmanEncoder;

/**
 * Container for a 1 Square-KM Raster Tile of 1m Resolution Lidar Data, can be
 * DOM1 (Digitales Oberflaechen Modell) or DGM1 (Digitales Gelaende Modell)
 *
 * Coordinate System is UTM (Universal Transverse Mercator)
 *
 * NODATA pixels are treated as 0-elevation for simplicty
 * (don't know if NODATA is used in DEM1 data)
 */
public class DEM1LidarTile {

    private int[] data; // array holding the elevation data in centi-meter
    private String dataType; // "dom1" or "dgm1"
    private int utmZone; // the UTM zone the x-coordinates refer to (usually 32)
    private int xBaseKm; // x-coordinate of the lower-left corner in KM
    private int yBaseKm; // y-coordinate of the lower-left corner in KM
    private int resolution; // resolution in meter (should be 1)

    public int[] getData() {
        return data;
    }

    public String getDataType() {
        return dataType;
    }

    public int getUtmZone() {
        return utmZone;
    }

    public int getXBaseKm() {
        return xBaseKm;
    }

    public int getYBaseKm() {
        return yBaseKm;
    }

    public int getResolution() {
        return resolution;
    }

    public boolean parseMetadataFromFileName(String fileName) {

        if (fileName.contains(".xyz")) {
            try {
                StringTokenizer tk = new StringTokenizer(fileName, "_");
                String type = tk.nextToken();
                utmZone = Integer.parseInt(tk.nextToken());
                xBaseKm = Integer.parseInt(tk.nextToken());
                yBaseKm = Integer.parseInt(tk.nextToken());
                resolution = Integer.parseInt(tk.nextToken());
                dataType = type; // assign late to have that as a valid marker
                return true;
            } catch (NumberFormatException nfe) {
                return false;
            }
        }
        return false;
    }

    
    /**
     * Read lidar from a stream in XYZ text format<br>
     * Expects 1000*1000 pixels (1 Square km, 1m resolution)<br>
     * <br>
     * Example data line is:<br> {@code 428007.00 5594000.00  194.83}
     *
     * @param inputStream the input stream to read from
     */
    public void readXYZData(InputStream inputStream) throws IOException {

        if (dataType == null) {
            throw new RuntimeException("use parseMetadataFromFileName() before reading data");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        data = new int[1000000];
        for (;;) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            if (line.isEmpty()) {
                continue;
            }
            StringTokenizer tk = new StringTokenizer(line, " ");
            String x = tk.nextToken();
            String y = tk.nextToken();
            String z = tk.nextToken();
            int xval = (int) (Double.parseDouble(x) + 0.5) - 1000 * xBaseKm;
            int yval = (int) (Double.parseDouble(y) + 0.5) - 1000 * yBaseKm;
            int zval = (int) (Double.parseDouble(z) * 100 + 0.5);
            if (xval < 0 || xval >= 1000 || yval < 0 || yval >= 1000) {
                throw new IllegalArgumentException("coordinates out of bound: " + line);
            }
            data[xval + yval * 1000] = zval;
        }
    }

    /**
     * Write this tile to an outputstream in a compact format<br>
     * <br>
     * It uses huffman encoding with fixed statistics on the elevation-diffs.
     *
     * @param outStream the stream to write to (expected to be buffered)
     * @see #writeCompact(InputStream)
     */
    public void writeCompact(OutputStream outStream) throws IOException {

        BitOutputStream bos = new BitOutputStream(outStream);
        bos.writeUTF(dataType);
        bos.encodeVarBytes(utmZone);
        bos.encodeVarBytes(xBaseKm);
        bos.encodeVarBytes(yBaseKm);
        bos.encodeVarBytes(resolution);

        LongEncoder encoder = new LongEncoder();
        for (int pass = 1; pass <= 2; pass++) {
            encoder.init(bos);
            long lastValue = 0L;
            for (int i = 0; i < 1000000; i++) {
                long value = data[i];
                encoder.encodeObject(Long.valueOf(value - lastValue));
                lastValue = value;
            }
        }
        bos.writeSyncBlock(0L);
    }

    private static class LongEncoder extends HuffmanEncoder {
        @Override
        protected void encodeObjectToStream(Object obj) throws IOException {
            long lv = ((Long) obj).longValue();
            bos.encodeSignedVarBits(lv, 8);
        }
    }

    /**
     * Fill this tile to an inputstream in a compact format<br>
     *
     * @param inStream the stream to read from (expected to be buffered)
     * @see #writeCompact(OutputStream)
     */
    public void readCompact(InputStream inStream) throws IOException {

        BitInputStream bis = new BitInputStream(inStream);
        dataType = bis.readUTF();
        utmZone = (int) bis.decodeVarBytes();
        xBaseKm = (int) bis.decodeVarBytes();
        yBaseKm = (int) bis.decodeVarBytes();
        resolution = (int) bis.decodeVarBytes();

        LongDecoder decoder = new LongDecoder();
        decoder.init(bis, 12);
        data = new int[1000000];
        long value = 0L;
        for (int i = 0; i < 1000000; i++) {
            value += ((Long) decoder.decodeObject()).longValue();
            data[i] = (int) value;
        }
        if (bis.readSyncBlock() != 0L) {
            throw new IllegalArgumentException("0-sync not found!");
        }
    }

    private static class LongDecoder extends HuffmanDecoder {
        @Override
        protected Object decodeObjectFromStream() throws IOException {
            long lv = bis.decodeSignedVarBits(8);
            return Long.valueOf(lv);
        }
    }

}
