package no.nav.melosys.integrasjon.eessi.dto;


public class Virksomhet {

    private String navn;
    private Adresse adresse;
    private String orgnr;
    private String type; //Trenger kanskje ikke denne?

    public Virksomhet() {}

    public Virksomhet(String navn, String orgnr, Adresse adresse) {
        this.navn = navn;
        this.orgnr = orgnr;
        this.adresse = adresse;
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

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
