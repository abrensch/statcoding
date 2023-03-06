import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;
import btools.statcoding.PrefixedBitInputStream;
import btools.statcoding.PrefixedBitOutputStream;

/**
 * Container for something like the DCC (European Digital Covid Certificate)
 */
public class CovidCertificate {

    public static long majorVersion = 1;
    public static long minorVersion = 1;

    public String firstName;
    public String lastName;
    public String standardName;
    public String dateOfBirth;

    public List<VaccinationEntry> vaccinationEntries;


    /**
     * Encode this certificate to a bit-stream
     */
    public void writeToStream( BitOutputStream bos ) {
        try( PrefixedBitOutputStream os = new PrefixedBitOutputStream( bos, majorVersion , minorVersion ) {
            os.encodeString( firstName );
            os.encodeString( lastName );
            os.encodeString( standardName );
            os.encodeString( dateOfBirth );
            int nv = vaccinationEntries == null ? 0 : vaccinationEntries.size();
            os.encodeVarBytes( nv );
            for( int i=0; i<nv; i++ ) {
                vaccinationEntries.get(i).writeToStream( os );
            }
        }
    }

    /**
     * Decode a certificate from a bit-stream
     */
    public CovidCertificate readFromStream( BitInputStream bis ) {
        try( PrefixedBitOutputStream os = new PrefixedBitOutputStream( bos, majorVersion , minorVersion ) {
            os.encodeString( firstName );
            os.encodeString( lastName );
            os.encodeString( standardName );
            os.encodeString( dateOfBirth );
            int nv = vaccinationEntries == null ? 0 : vaccinationEntries.size();
            os.encodeVarBytes( nv );
            for( int i=0; i<nv; i++ ) {
                vaccinationEntries.get(i).writeToStream( os );
            }
        }
    }


    
}
