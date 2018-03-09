package no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.kodeverk;

import no.nav.melosys.domain.dokument.KodeverkEnum;

/**
 * Denne enumen er en hardkoding av kodeverket Fagomrade:
 * https://kodeverkviewer.adeo.no/kodeverk/xml/fagomrade.xml
 */
public enum Fagomrade implements KodeverkEnum<Fagomrade> {
    MED("Medlemskap"),
    UFM("Unntak fra medlemskap");

    private String navn;

    Fagomrade(String navn) {
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
