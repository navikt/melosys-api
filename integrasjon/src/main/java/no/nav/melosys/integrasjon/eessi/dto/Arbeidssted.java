package no.nav.melosys.integrasjon.eessi.dto;

import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;

public class Arbeidssted {

    private String navn;
    private Adresse adresse;
    private boolean fysisk;
    private String hjemmebase;

    public ArbeidUtland tilArbeidUtland() {
        ArbeidUtland arbeidUtland = new ArbeidUtland();

        arbeidUtland.adresse = adresse.tilStrukturertAdresse();
        arbeidUtland.foretakNavn = navn;

        return arbeidUtland;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public Adresse getAdresse() {
        return adresse;
    }

    public void setAdresse(Adresse adresse) {
        this.adresse = adresse;
    }

    public boolean isFysisk() {
        return fysisk;
    }

    public void setFysisk(boolean fysisk) {
        this.fysisk = fysisk;
    }

    public String getHjemmebase() {
        return hjemmebase;
    }

    public void setHjemmebase(String hjemmebase) {
        this.hjemmebase = hjemmebase;
    }
}
