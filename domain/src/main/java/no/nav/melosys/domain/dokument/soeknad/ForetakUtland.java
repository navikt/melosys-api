package no.nav.melosys.domain.dokument.soeknad;

/**
 * Opplysninger om foretak i utlandet
 */
public class ForetakUtland {
    public String navn;
    public String orgnr;
    public StandardAdresse adresse = new StandardAdresse();
}
