package no.nav.melosys.domain.eessi.melding;

public class Arbeidssted {
    public String navn;
    public Adresse adresse;
    public String hjemmebase;
    public boolean erIkkeFastAdresse;

    public Arbeidssted(String navn, Adresse adresse) {
        this.navn = navn;
        this.adresse = adresse;
    }
}
