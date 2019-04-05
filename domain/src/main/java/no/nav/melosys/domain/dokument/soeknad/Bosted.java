package no.nav.melosys.domain.dokument.soeknad;

import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;

public class Bosted {
    public Boolean intensjonOmRetur;
    public String familiesBostedLandkode;
    public int antallMaanederINorge;
    public Boolean adresseIUtlandet;
    public StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
}
