package no.nav.melosys.integrasjon.eessi.dto;


public class Arbeidssted {

    private String navn;
    private Adresse adresse;
    private boolean fysisk;
    private String hjemmebase;

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
