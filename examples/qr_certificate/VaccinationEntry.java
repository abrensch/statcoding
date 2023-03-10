import java.io.*;
import java.util.*;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;
import btools.statcoding.PrefixedBitInputStream;
import btools.statcoding.PrefixedBitOutputStream;

/**
 * Container for something like a vaccination as part of the EU-DCC (European Digital Covid Certificate)
 */
public class VaccinationEntry {

    public static long majorVersion = 1;
    public static long minorVersion = 1;

    public String targetDesease;
    public String vaccineType;
    public String vaccineProduct;
    public String manufacturer;
    public long doseNumber;
    public long seriesDoses;
    public String vaccinationDate;
    public String vaccinationCountry;
    public String certificateIssuer;
    public String certificateID;

    public VaccinationEntry() {
    }

    /**
     * Encode this vaccination-entry to a bit-stream
     */
    public void writeToStream(BitOutputStream bos) throws IOException {
        try (PrefixedBitOutputStream os = new PrefixedBitOutputStream(bos, majorVersion, minorVersion)) {
            os.encodeString(targetDesease);
            os.encodeString(vaccineType);
            os.encodeString(vaccineProduct);
            os.encodeString(manufacturer);
            os.encodeUnsignedVarBits(doseNumber,3);
            os.encodeUnsignedVarBits(seriesDoses,3);
            os.encodeString(vaccinationDate);
            os.encodeString(vaccinationCountry);
            os.encodeString(certificateIssuer);
            os.encodeString(certificateID);
        }
    }

    /**
     * Decode a vaccination-entry from a bit-stream
     */
    public VaccinationEntry(BitInputStream bis) throws IOException {
        try (PrefixedBitInputStream is = new PrefixedBitInputStream(bis, majorVersion)) {
            targetDesease = is.decodeString();
            vaccineType = is.decodeString();
            vaccineProduct = is.decodeString();
            manufacturer = is.decodeString();
            doseNumber = is.decodeUnsignedVarBits(3);
            seriesDoses = is.decodeUnsignedVarBits(3);
            vaccinationDate = is.decodeString();
            vaccinationCountry = is.decodeString();
            certificateIssuer = is.decodeString();
            certificateID = is.decodeString();
        }
    }

    @Override
    public String toString() {
        return "\n*** Vaccination entry ***"
        + "\ntargetDesease=" + targetDesease
        + "\nvaccineType=" + vaccineType
        + "\nvaccineProduct=" + vaccineProduct
        + "\nmanufacturer=" + manufacturer
        + "\ndoseNumber=" + doseNumber
        + "\nseriesDoses=" + seriesDoses
        + "\nvaccinationDate=" + vaccinationDate
        + "\nvaccinationCountry=" + vaccinationCountry
        + "\ncertificateIssuer=" + certificateIssuer
        + "\ncertificateID=" + certificateID
        ;
    }
}
