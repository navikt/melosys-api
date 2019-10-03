package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

public abstract class AbstractArbeidssted implements Arbeidssted {
    protected final String foretakNavn;
    protected final String landkode;
    protected final String idnummer;

    protected AbstractArbeidssted(String foretakNavn, String idnummer, String landkode) {
        this.foretakNavn = foretakNavn;
        this.idnummer = idnummer;
        this.landkode = landkode;
    }

    @Override
    public String getForetakNavn() {
        return foretakNavn;
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
