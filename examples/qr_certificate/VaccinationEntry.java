import java.io.*;
import java.util.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;
import btools.statcoding.PrefixedBitInputStream;
import btools.statcoding.PrefixedBitOutputStream;

/**
 * Container for something like the DCC (European Digital Covid Certificate)
 */
public class VaccinationEntry {

    public static long majorVersion = 1;
    public static long minorVersion = 1;

    public String certificateID;
    public String country;
    public String vaccinationDate;
    public String certificateIssuer;
    public String targetDesease;
    public String manufacturer;
    public String vaccineName;
    public String vaccineType;

    public VaccinationEntry() {
    }

    /**
     * Encode this vaccination-entry to a bit-stream
     */
    public void writeToStream(BitOutputStream bos) throws IOException {
        try (PrefixedBitOutputStream os = new PrefixedBitOutputStream(bos, majorVersion, minorVersion)) {
            os.encodeString(certificateID);
            os.encodeString(country);
            os.encodeString(vaccinationDate);
            os.encodeString(certificateIssuer);
            os.encodeString(targetDesease);
            os.encodeString(manufacturer);
            os.encodeString(vaccineName);
            os.encodeString(vaccineType);
        }
    }

    /**
     * Decode a vaccination-entry from a bit-stream
     */
    public VaccinationEntry(BitInputStream bis) throws IOException {
        try (PrefixedBitInputStream is = new PrefixedBitInputStream(bis, majorVersion)) {
            certificateID = is.decodeString();
            country = is.decodeString();
            vaccinationDate = is.decodeString();
            certificateIssuer = is.decodeString();
            targetDesease = is.decodeString();
            manufacturer = is.decodeString();
            vaccineName = is.decodeString();
            vaccineType = is.decodeString();
        }
    }

    public String toString() {
        return "\n*** Vaccination entry ***" + "\ncertificateID=" + certificateID + "\ncountry=" + country
                + "\nvaccinationDate=" + vaccinationDate + "\ncertificateIssuer=" + certificateIssuer
                + "\ntargetDesease=" + targetDesease + "\nmanufacturer=" + manufacturer + "\nvaccineName=" + vaccineName
                + "\nvaccineType=" + vaccineType;
    }
}
