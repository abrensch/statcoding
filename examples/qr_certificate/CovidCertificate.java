import java.io.*;
import java.util.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;
import btools.statcoding.PrefixedBitInputStream;
import btools.statcoding.PrefixedBitOutputStream;

/**
 * Container for something like the EU-DCC (European Digital Covid Certificate)
 */
public class CovidCertificate {

    public static long majorVersion = 1;
    public static long minorVersion = 1;

    public String country;
    public long timeIssued;
    public long timeValidUntil;
    public String familyName;
    public String familyNameT;
    public String givenName;
    public String givenNameT;
    public String dateOfBirth;


    public final List<VaccinationEntry> vaccinationEntries = new ArrayList<>();

    public CovidCertificate() {
    }

    /**
     * Encode this certificate to a bit-stream
     */
    public void writeToStream(BitOutputStream bos) throws IOException {
        try (PrefixedBitOutputStream os = new PrefixedBitOutputStream(bos, majorVersion, minorVersion)) {
            os.encodeString(country);
            os.encodeSignedVarBits(timeIssued,32);
            os.encodeSignedVarBits(timeValidUntil,32);
            os.encodeString(familyName);
            os.encodeString(familyNameT);
            os.encodeString(givenName);
            os.encodeString(givenNameT);
            os.encodeString(dateOfBirth);
            os.encodeUnsignedVarBits(vaccinationEntries.size(),1);
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
            country = is.decodeString();
            timeIssued = is.decodeSignedVarBits(32);
            timeValidUntil = is.decodeSignedVarBits(32);
            familyName = is.decodeString();
            familyNameT = is.decodeString();
            givenName = is.decodeString();
            givenNameT = is.decodeString();
            dateOfBirth = is.decodeString();
            long nv = is.decodeUnsignedVarBits(1);
            for (long i = 0; i < nv; i++) {
                vaccinationEntries.add(new VaccinationEntry(is));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n*** Covid Certificate contents ***"
        + "\ncountry=" + country
        + "\ntimeIssued=" + new Date(1000L*timeIssued)
        + "\ntimeValidUntil=" + new Date(1000L*timeValidUntil)
        + "\nfamilyName=" + familyName
        + "\nfamilyNameT=" + familyNameT
        + "\ngivenName=" + givenName
        + "\ngivenNameT=" + givenNameT
        + "\ndateOfBirth=" + dateOfBirth);
        for (VaccinationEntry ve : vaccinationEntries) {
            sb.append("\n").append(ve);
        }
        return sb.toString();
    }
}
