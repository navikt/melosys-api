package no.nav.melosys.domain.dokument.soeknad;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Bosted {
    public boolean intensjonOmRetur;
    public boolean bostedUtenforNorge;
    public String familiesBostedLandKode;
    public int antallMaanederINorge;
    @JsonProperty("EOSBarnetrygdFraNAV")
    public boolean EØSBarnetrygdFraNAV;
    public boolean adresseIUtlandet;
    public StandardAdresse oppgittAdresse;
}
