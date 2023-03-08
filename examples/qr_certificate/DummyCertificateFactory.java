/**
 * Factory for a dummy certificate
 */
public class DummyCertificateFactory {

    public static CovidCertificate getInstance() {
        CovidCertificate c = new CovidCertificate();
        c.firstName = "Dagobert";
        c.lastName = "Duck";
        c.standardName = "DAGOBERT DUCK";
        c.dateOfBirth = "1958-06-08";

        VaccinationEntry e = new VaccinationEntry();
        e.certificateID = java.util.UUID.randomUUID().toString();
        e.country = "DE";
        e.vaccinationDate = "2022-02-13";
        e.certificateIssuer = "Robert Koch Institut";
        e.targetDesease = "Covid19";
        e.manufacturer = "Biontech";
        e.vaccineName = "Comirnaty";
        e.vaccineType = "SARS-CoV-2 mNA vaccine";

        c.vaccinationEntries.add(e);
        return c;
    }
}
