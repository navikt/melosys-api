package no.nav.melosys.service.dokument.brev.mapper.felles;

import no.nav.melosys.domain.kodeverk.Yrkesgrupper;

public class Arbeidssted {
    public Arbeidssted(String navn, String idnummer, String landkode) {
        this.navn = navn;
        this.idnummer = idnummer;
        this.landkode = landkode;
    }

    public boolean erFysisk() {
        return false;
    }

    public String getNavn() {
        return navn;
    }

    /**
     * Område tilsvarer landkode for fysiske arbeidssteder,
     * men er en formatert landkode for maritimtarbeidssted.
     * Eks: for sokkel "offshore, <landkode>"
     **/
    public String getOmråde() {
        return getLandkode();
    }

    public String getLandkode() {
        return  landkode;
    }

    public String getIdnummer() {
        return idnummer;
    }

    public Yrkesgrupper getYrkesgruppe() {
        return Yrkesgrupper.ORDINAER;
    }

    protected final String navn;
    protected final String landkode;
    protected final String idnummer;
}
