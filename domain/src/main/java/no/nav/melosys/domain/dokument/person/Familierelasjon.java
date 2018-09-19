package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;

/**
 * Denne enumen er en hardkoding av kodeverket Familierelasjoner.
 */
public enum Familierelasjon implements KodeverkHjelper {
    EKTE("EKTE"),
    SAM("SAM"),
    FARA("FARA"),
    REPA("REPA"),
    BARN("BARN"),
    MORA("MORA");

    private String kode;

    Familierelasjon(String kode) {
        this.kode = kode;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.FAMILIERELASJONER;
    }
}
