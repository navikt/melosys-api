package no.nav.melosys.domain.dokument.soeknad;

public class Bosted {
    public boolean intensjonOmRetur;
    public boolean bostedUtenforNorge;
    public String familiesBostedLandKode;
    public int antallMaanederINorge;
    public boolean adresseIUtlandet;
    public StandardAdresse oppgittAdresse = new StandardAdresse();
}
