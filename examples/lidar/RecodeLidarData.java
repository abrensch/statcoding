import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Look for ZIPs with Lidar data in the current directory and recode them for
 * faster access.
 */
public class RecodeLidarData {

    public static void main(String[] args) throws Exception {

        File dataDir = new File("data");
        if ( !dataDir.isDirectory() && !dataDir.mkdir() ) {
            throw new IllegalArgumentException( "cannot create data-dir: " + dataDir );
        }
        File[] files = new File(".").listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".zip")) {
                try ( ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(f.getName()))) ) {
                    for (;;) {
                        ZipEntry ze = zis.getNextEntry();
                        if (ze == null) {
                            break;
                        }
                        if (ze.getName().endsWith(".xyz")) {
                            DEM1LidarTile tile = new DEM1LidarTile();
                            if (!tile.parseMetadataFromFileName(ze.getName())) {
                                continue;
                            }
                            System.out.println("reading " + f + "-->" + ze.getName());
                            tile.readXYZData(zis);
                            File outFile = new File(dataDir, ze.getName() + ".lz");
                            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile))) {
                                tile.writeCompact(os);
                            }
                        }
                    }
                }
            }
        }
    }
}
