package no.nav.melosys.integrasjon.gsak.kodeverk;

/**
 * Denne enumen er en hardkoding av kodeverket Fagomrade:
 * https://kodeverkviewer.adeo.no/kodeverk/xml/fagomrade.xml
 */
public enum Fagomrade {
    MED("Medlemskap"),
    UFM("Unntak fra medlemskap");

    private String navn;

    Fagomrade(String navn) {
        this.navn = navn;
    }
}
