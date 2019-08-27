package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

public abstract class AbstractArbeidssted implements Arbeidssted {
    protected final String navn;
    protected final String landkode;
    protected final String idnummer;

    public AbstractArbeidssted(String navn, String idnummer, String landkode) {
        this.navn = navn;
        this.idnummer = idnummer;
        this.landkode = landkode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getLandkode() {
        return  landkode;
    }

    @Override
    public String getIdnummer() {
        return idnummer;
    }
}
