/**
 * Factory for a dummy certificate
 */
public class DummyCertificateFactory {

    public static CovidCertificate getInstance() {

        CovidCertificate c = new CovidCertificate();
        c.country = "NL";
        c.timeIssued = 1621755800L;
        c.timeValidUntil = 1621755800L;
        c.familyName = "Achternaam";
        c.familyNameT = "ACHTERNAAM";
        c.givenName = "Voornaam";
        c.givenNameT = "VOORNAAM";
        c.dateOfBirth = "1963-01-01";

        VaccinationEntry e = new VaccinationEntry();
        e.targetDesease = "840539006";
        e.vaccineType = "1119305005";
        e.vaccineProduct = "CVnCoV";
        e.manufacturer = "ORG-100032020";
        e.doseNumber = 1L;
        e.seriesDoses = 6L;
        e.vaccinationDate = "2021-02-18";
        e.vaccinationCountry = "GR";
        e.certificateIssuer = "Ministry of Health Welfare and Sport";
        e.certificateID = "urn:uvci:01:NL:74827831729545bba1c279f592f2488a";

        c.vaccinationEntries.add(e);
        return c;
    }
}
