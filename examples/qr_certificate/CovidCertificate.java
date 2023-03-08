import java.io.*;
import java.util.*;

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

    public final List<VaccinationEntry> vaccinationEntries = new ArrayList<>();

    public CovidCertificate() {
    }

    /**
     * Encode this certificate to a bit-stream
     */
    public void writeToStream(BitOutputStream bos) throws IOException {
        try (PrefixedBitOutputStream os = new PrefixedBitOutputStream(bos, majorVersion, minorVersion)) {
            os.encodeString(firstName);
            os.encodeString(lastName);
            os.encodeString(standardName);
            os.encodeString(dateOfBirth);
            os.encodeVarBytes(vaccinationEntries.size());
            for (VaccinationEntry ve : vaccinationEntries) {
                ve.writeToStream(os);
            }
        }
    }

    /**
     * Decode a certificate from a bit-stream
     */
    public CovidCertificate(BitInputStream bis) throws IOException {
        try (PrefixedBitInputStream is = new PrefixedBitInputStream(bis, majorVersion)) {
            firstName = is.decodeString();
            lastName = is.decodeString();
            standardName = is.decodeString();
            dateOfBirth = is.decodeString();
            long nv = is.decodeVarBytes();
            for (long i = 0; i < nv; i++) {
                vaccinationEntries.add(new VaccinationEntry(is));
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("\n*** Covid Certificate contents ***" + "\nfirstName=" + firstName
                + "\nlastName=" + lastName + "\nstandardName=" + standardName + "\ndateOfBirth=" + dateOfBirth);
        for (VaccinationEntry ve : vaccinationEntries) {
            sb.append("\n").append(ve);
        }
        return sb.toString();
    }
}
