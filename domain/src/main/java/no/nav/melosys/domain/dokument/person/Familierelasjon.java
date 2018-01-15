package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.dokument.KodeverkEnum;

/**
 * Denne enumen er en hardkoding av kodeverket Familierelasjoner.
 */
public enum Familierelasjon implements KodeverkEnum<Familierelasjon> {
    EKTE("Ektefelle til"),
    SAM("Samboer med"),
    FARA("Far til"),
    REPA("Registrert partner med"),
    BARN("Barn av"),
    MORA("Mor til");

    private String navn;

    Familierelasjon(String navn) {
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
